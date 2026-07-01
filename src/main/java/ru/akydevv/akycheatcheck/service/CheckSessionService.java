package ru.akydevv.akycheatcheck.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import ru.akydevv.akycheatcheck.api.event.AkyCheckEndEvent;
import ru.akydevv.akycheatcheck.api.event.AkyCheckStartEvent;
import ru.akydevv.akycheatcheck.config.SettingsProvider;
import ru.akydevv.akycheatcheck.model.CheckCabin;
import ru.akydevv.akycheatcheck.model.CheckEndReason;
import ru.akydevv.akycheatcheck.model.CheckSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CheckSessionService {
    private final Plugin plugin;
    private final SettingsProvider settingsProvider;
    private final MessageService messageService;
    private final PunishmentService punishmentService;
    private final TrophyService trophyService;
    private final ModeratorStatisticsService statisticsService;
    private final AnticheatCompatibilityService compatibilityService;
    private final ClientBrandService clientBrandService;
    private final CheckAuditService checkAuditService;
    private final ConcurrentMap<UUID, CheckSession> sessionsBySuspect = new ConcurrentHashMap<>();
    private BukkitTask timerTask;

    public CheckSessionService(Plugin plugin,
                               SettingsProvider settingsProvider,
                               MessageService messageService,
                               PunishmentService punishmentService,
                               TrophyService trophyService,
                               ModeratorStatisticsService statisticsService,
                               AnticheatCompatibilityService compatibilityService,
                               ClientBrandService clientBrandService,
                               CheckAuditService checkAuditService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
        this.messageService = Objects.requireNonNull(messageService, "messageService");
        this.punishmentService = Objects.requireNonNull(punishmentService, "punishmentService");
        this.trophyService = Objects.requireNonNull(trophyService, "trophyService");
        this.statisticsService = Objects.requireNonNull(statisticsService, "statisticsService");
        this.compatibilityService = Objects.requireNonNull(compatibilityService, "compatibilityService");
        this.clientBrandService = Objects.requireNonNull(clientBrandService, "clientBrandService");
        this.checkAuditService = Objects.requireNonNull(checkAuditService, "checkAuditService");
    }

    public void startTimer() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSessions, 20L, 20L);
    }

    public boolean startCheck(Player moderator, Player suspect, long requestedDurationSeconds) {
        if (moderator.getUniqueId().equals(suspect.getUniqueId())) {
            messageService.send(moderator, "&cНельзя вызвать на проверку самого себя.");
            return false;
        }
        if (sessionsBySuspect.containsKey(suspect.getUniqueId())) {
            messageService.send(moderator, "&cЭтот игрок уже находится на проверке.");
            return false;
        }
        if (isModeratorBusyWith(suspect.getUniqueId())) {
            messageService.send(moderator, "&cКонфликт сессий проверки.");
            return false;
        }
        Optional<CheckCabin> cabinOptional = findAvailableCabin();
        if (cabinOptional.isEmpty()) {
            messageService.send(moderator, "&cНет свободной кабинки проверки или мир кабинки не загружен.");
            return false;
        }
        CheckCabin cabin = cabinOptional.get();
        Optional<Location> cabinLocation = cabin.toLocation();
        if (cabinLocation.isEmpty()) {
            messageService.send(moderator, "&cМир кабинки '&f" + cabin.worldName() + "&c' не загружен.");
            return false;
        }

        long duration = Math.min(Math.max(1L, requestedDurationSeconds), settingsProvider.getSettings().getMaxDurationSeconds());
        BossBar bossBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SEGMENTED_12);
        bossBar.addPlayer(suspect);
        bossBar.addPlayer(moderator);

        CheckSession session = new CheckSession(
                suspect.getUniqueId(),
                moderator.getUniqueId(),
                suspect.getName(),
                moderator.getName(),
                cabin,
                suspect.getLocation(),
                Instant.now(),
                duration,
                bossBar
        );
        CheckSession previous = sessionsBySuspect.putIfAbsent(suspect.getUniqueId(), session);
        if (previous != null) {
            bossBar.removeAll();
            messageService.send(moderator, "&cЭтот игрок уже находится на проверке.");
            return false;
        }

        compatibilityService.applyCheckProtection(suspect);
        suspect.teleport(cabinLocation.get());
        moderator.teleport(cabinLocation.get());
        statisticsService.recordCheck(moderator);
        updateBossBar(session, Instant.now());
        Bukkit.getPluginManager().callEvent(new AkyCheckStartEvent(moderator, suspect, cabin, duration));
        checkAuditService.logStart(moderator.getName(), suspect.getName(), clientBrandService.describeClient(suspect.getUniqueId()), cabin.id(), duration);

        Map<String, String> placeholders = Map.of(
                "player", suspect.getName(),
                "moderator", moderator.getName(),
                "time", formatTime(duration),
                "client", clientBrandService.describeClient(suspect.getUniqueId())
        );
        messageService.send(moderator, messageService.format("&aПроверка начата. Клиент игрока: &f%client%", placeholders));
        messageService.send(suspect, messageService.format("&cВы вызваны на проверку модератором &f%moderator%&c. Пишите только в чат.", placeholders));
        return true;
    }

    public boolean release(Player moderator, Player suspect) {
        CheckSession session = sessionsBySuspect.get(suspect.getUniqueId());
        if (session == null) {
            messageService.send(moderator, "&cИгрок не находится на проверке.");
            return false;
        }
        if (!session.getModeratorId().equals(moderator.getUniqueId())) {
            messageService.send(moderator, "&cЭту проверку ведёт &f" + session.getModeratorName() + "&c.");
            return false;
        }
        endSession(session, CheckEndReason.RELEASED, "Проверка пройдена", moderator);
        return true;
    }

    public boolean ban(Player moderator, Player suspect, String reason, CheckEndReason reasonType) {
        CheckSession session = sessionsBySuspect.get(suspect.getUniqueId());
        if (session == null) {
            messageService.send(moderator, "&cИгрок не находится на проверке.");
            return false;
        }
        if (!session.getModeratorId().equals(moderator.getUniqueId())) {
            messageService.send(moderator, "&cЭту проверку ведёт &f" + session.getModeratorName() + "&c.");
            return false;
        }
        endSession(session, reasonType, reason, moderator);
        return true;
    }

    public void banBySystem(UUID suspectId, CheckEndReason reasonType, String reason) {
        CheckSession session = sessionsBySuspect.get(suspectId);
        if (session != null) {
            endSession(session, reasonType, reason, Bukkit.getPlayer(session.getModeratorId()));
        }
    }

    public boolean addTime(Player moderator, Player suspect, long seconds) {
        CheckSession session = sessionsBySuspect.get(suspect.getUniqueId());
        if (session == null) {
            messageService.send(moderator, "&cИгрок не находится на проверке.");
            return false;
        }
        if (!session.getModeratorId().equals(moderator.getUniqueId())) {
            messageService.send(moderator, "&cЭту проверку ведёт &f" + session.getModeratorName() + "&c.");
            return false;
        }
        long allowedSeconds = Math.min(seconds, Math.max(0L, settingsProvider.getSettings().getMaxDurationSeconds() - session.getDurationSeconds()));
        if (allowedSeconds <= 0L) {
            messageService.send(moderator, "&cНельзя превысить максимальное время проверки из конфига.");
            return false;
        }
        session.addDurationSeconds(allowedSeconds);
        updateBossBar(session, Instant.now());
        messageService.send(moderator, "&aДобавлено времени: &f" + formatTime(allowedSeconds));
        messageService.send(suspect, "&eМодератор добавил время проверки: &f" + formatTime(allowedSeconds));
        return true;
    }

    public Optional<CheckSession> findBySuspect(UUID suspectId) {
        return Optional.ofNullable(sessionsBySuspect.get(suspectId));
    }

    public Optional<CheckSession> findByParticipant(UUID playerId) {
        CheckSession suspectSession = sessionsBySuspect.get(playerId);
        if (suspectSession != null) {
            return Optional.of(suspectSession);
        }
        return sessionsBySuspect.values().stream()
                .filter(session -> session.getModeratorId().equals(playerId))
                .findFirst();
    }

    public boolean isSuspect(UUID playerId) {
        return sessionsBySuspect.containsKey(playerId);
    }

    public boolean isModerator(UUID playerId) {
        return sessionsBySuspect.values().stream().anyMatch(session -> session.getModeratorId().equals(playerId));
    }

    public Collection<CheckSession> getSessions() {
        return List.copyOf(sessionsBySuspect.values());
    }

    public int getActiveSessionCount() {
        return sessionsBySuspect.size();
    }

    public void shutdown() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        for (CheckSession session : new ArrayList<>(sessionsBySuspect.values())) {
            closeWithoutPunishment(session);
        }
        sessionsBySuspect.clear();
    }

    private void closeWithoutPunishment(CheckSession session) {
        sessionsBySuspect.remove(session.getSuspectId());
        session.getBossBar().removeAll();
        Player suspect = Bukkit.getPlayer(session.getSuspectId());
        if (suspect != null && suspect.isOnline()) {
            compatibilityService.removeCheckProtection(suspect);
            suspect.teleport(session.getReturnLocation());
            messageService.send(suspect, "&eПроверка остановлена из-за отключения сервера.");
        }
    }

    private boolean isModeratorBusyWith(UUID suspectId) {
        return sessionsBySuspect.values().stream().anyMatch(session -> session.getModeratorId().equals(suspectId));
    }

    private Optional<CheckCabin> findAvailableCabin() {
        Map<String, Long> usedCabins = new HashMap<>();
        for (CheckSession session : sessionsBySuspect.values()) {
            usedCabins.merge(session.getCabin().id(), 1L, Long::sum);
        }
        return settingsProvider.getSettings().getCabins().stream()
                .filter(cabin -> cabin.toLocation().isPresent())
                .filter(cabin -> !usedCabins.containsKey(cabin.id()))
                .min(Comparator.comparing(CheckCabin::id));
    }

    private void tickSessions() {
        Instant now = Instant.now();
        for (CheckSession session : new ArrayList<>(sessionsBySuspect.values())) {
            updateBossBar(session, now);
            if (session.remainingSeconds(now) <= 0L && settingsProvider.getSettings().isAutoBanOnTimeout()) {
                endSession(session, CheckEndReason.TIMEOUT, "Игнор проверки", Bukkit.getPlayer(session.getModeratorId()));
            }
        }
    }

    private void updateBossBar(CheckSession session, Instant now) {
        long remaining = session.remainingSeconds(now);
        String title = messageService.format(settingsProvider.getSettings().getBossBarTitle(), Map.of("time", formatTime(remaining)));
        session.getBossBar().setTitle(title);
        double progress = session.getDurationSeconds() <= 0L ? 0.0D : (double) remaining / (double) session.getDurationSeconds();
        session.getBossBar().setProgress(Math.max(0.0D, Math.min(1.0D, progress)));
    }

    private void endSession(CheckSession session, CheckEndReason reasonType, String reason, Player moderator) {
        CheckSession removed = sessionsBySuspect.remove(session.getSuspectId());
        if (removed == null) {
            return;
        }
        Player suspect = Bukkit.getPlayer(session.getSuspectId());
        session.getBossBar().removeAll();
        Bukkit.getPluginManager().callEvent(new AkyCheckEndEvent(
                session.getSuspectId(),
                session.getSuspectName(),
                Bukkit.getOfflinePlayer(session.getModeratorId()),
                reasonType,
                reason
        ));
        checkAuditService.logEnd(session.getModeratorName(), session.getSuspectName(), reasonType, reason);

        if (suspect != null && suspect.isOnline()) {
            compatibilityService.removeCheckProtection(suspect);
        }

        if (reasonType == CheckEndReason.RELEASED) {
            if (suspect != null && suspect.isOnline()) {
                suspect.teleport(session.getReturnLocation());
                trophyService.giveTrophy(suspect, session.getModeratorName());
                messageService.send(suspect, "&aПроверка завершена. Нарушений не найдено.");
            }
            if (moderator != null) {
                statisticsService.recordRelease(moderator);
            }
            messageService.broadcast(messageService.format(settingsProvider.getSettings().getReleaseBroadcast(), Map.of(
                    "player", session.getSuspectName(),
                    "moderator", session.getModeratorName(),
                    "reason", reason
            )));
            return;
        }

        if (suspect != null && suspect.isOnline()) {
            punishmentService.ban(suspect, reason);
        } else {
            String command = settingsProvider.getSettings().getBanCommand()
                    .replace("%player%", session.getSuspectName())
                    .replace("%reason%", reason)
                    .replaceFirst("^/", "");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            messageService.broadcast(messageService.format(settingsProvider.getSettings().getBanBroadcast(), Map.of(
                    "player", session.getSuspectName(),
                    "reason", reason,
                    "moderator", session.getModeratorName()
            )));
        }
        if (moderator != null) {
            statisticsService.recordBan(moderator);
        }
    }

    private String formatTime(long seconds) {
        long minutesPart = seconds / 60L;
        long secondsPart = seconds % 60L;
        return String.format("%02d:%02d", minutesPart, secondsPart);
    }
}
