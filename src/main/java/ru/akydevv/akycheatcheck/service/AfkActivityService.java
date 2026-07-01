package ru.akydevv.akycheatcheck.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import ru.akydevv.akycheatcheck.config.SettingsProvider;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AfkActivityService {
    private final Plugin plugin;
    private final SettingsProvider settingsProvider;
    private final MessageService messageService;
    private final CheckSessionService checkSessionService;
    private final ConcurrentMap<UUID, Long> lastActivityEpochSeconds = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> lastAutoCheckEpochSeconds = new ConcurrentHashMap<>();
    private BukkitTask scanTask;

    public AfkActivityService(Plugin plugin,
                              SettingsProvider settingsProvider,
                              MessageService messageService,
                              CheckSessionService checkSessionService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
        this.messageService = Objects.requireNonNull(messageService, "messageService");
        this.checkSessionService = Objects.requireNonNull(checkSessionService, "checkSessionService");
    }

    public void start() {
        stop();
        long periodTicks = settingsProvider.getSettings().getAfkScanPeriodSeconds() * 20L;
        scanTask = Bukkit.getScheduler().runTaskTimer(plugin, this::scan, periodTicks, periodTicks);
    }

    public void stop() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }

    public void markActivity(Player player) {
        lastActivityEpochSeconds.put(player.getUniqueId(), Instant.now().getEpochSecond());
    }

    public void forget(Player player) {
        lastActivityEpochSeconds.remove(player.getUniqueId());
        lastAutoCheckEpochSeconds.remove(player.getUniqueId());
    }

    public void clear() {
        stop();
        lastActivityEpochSeconds.clear();
        lastAutoCheckEpochSeconds.clear();
    }

    private void scan() {
        if (!settingsProvider.getSettings().isAfkEnabled()) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        long threshold = settingsProvider.getSettings().getAfkIdleSecondsBeforeCheck();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("akycheatcheck.bypass") || checkSessionService.findByParticipant(player.getUniqueId()).isPresent()) {
                continue;
            }
            long lastActivity = lastActivityEpochSeconds.getOrDefault(player.getUniqueId(), now);
            if (now - lastActivity < threshold) {
                continue;
            }
            long lastAutoCheck = lastAutoCheckEpochSeconds.getOrDefault(player.getUniqueId(), 0L);
            if (now - lastAutoCheck < threshold) {
                continue;
            }
            Optional<Player> moderator = findAvailableModerator(player);
            if (moderator.isPresent()) {
                lastAutoCheckEpochSeconds.put(player.getUniqueId(), now);
                boolean started = checkSessionService.startCheck(
                        moderator.get(),
                        player,
                        settingsProvider.getSettings().getDefaultDurationSeconds()
                );
                if (started) {
                    messageService.send(moderator.get(), "&eИгрок вызван автоматически из-за AFK: &f" + player.getName());
                }
            }
        }
    }

    private Optional<Player> findAvailableModerator(Player suspect) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player.class::cast)
                .filter(player -> !player.getUniqueId().equals(suspect.getUniqueId()))
                .filter(player -> player.hasPermission("akycheatcheck.moderator"))
                .filter(player -> checkSessionService.findByParticipant(player.getUniqueId()).isEmpty())
                .min(Comparator.comparing(Player::getName));
    }
}
