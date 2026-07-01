package ru.akydevv.akycheatcheck.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.akydevv.akycheatcheck.config.SettingsProvider;
import ru.akydevv.akycheatcheck.model.CheckEndReason;
import ru.akydevv.akycheatcheck.model.CheckSession;
import ru.akydevv.akycheatcheck.model.ModeratorStatistics;
import ru.akydevv.akycheatcheck.service.CheckSessionService;
import ru.akydevv.akycheatcheck.service.ClientBrandService;
import ru.akydevv.akycheatcheck.service.MessageService;
import ru.akydevv.akycheatcheck.service.ModeratorStatisticsService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class CheatCheckCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("start", "release", "as", "ban", "soft", "deny", "confess", "addtime", "setcabin", "info", "stats", "reload", "list");

    private final JavaPlugin plugin;
    private final SettingsProvider settingsProvider;
    private final CheckSessionService checkSessionService;
    private final ModeratorStatisticsService statisticsService;
    private final ClientBrandService clientBrandService;
    private final MessageService messageService;

    public CheatCheckCommand(JavaPlugin plugin,
                             SettingsProvider settingsProvider,
                             CheckSessionService checkSessionService,
                             ModeratorStatisticsService statisticsService,
                             ClientBrandService clientBrandService,
                             MessageService messageService) {
        this.plugin = plugin;
        this.settingsProvider = settingsProvider;
        this.checkSessionService = checkSessionService;
        this.statisticsService = statisticsService;
        this.clientBrandService = clientBrandService;
        this.messageService = messageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("akycheatcheck.moderator")) {
            messageService.send(sender, "&cНедостаточно прав.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> handleStart(sender, label, args);
            case "release", "as" -> handleRelease(sender, label, args);
            case "ban", "soft" -> handleBan(sender, label, args, CheckEndReason.MANUAL_BAN);
            case "deny" -> handleDeny(sender, label, args);
            case "confess" -> handleBan(sender, label, args, CheckEndReason.CONFESSION);
            case "addtime" -> handleAddTime(sender, label, args);
            case "setcabin" -> handleSetCabin(sender, args);
            case "info" -> handleInfo(sender, label, args);
            case "stats" -> handleStats(sender, args);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("akycheatcheck.moderator")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && List.of("start", "release", "as", "ban", "soft", "deny", "confess", "addtime", "info").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        return List.of();
    }

    private void handleStart(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player moderator)) {
            messageService.send(sender, "&cКоманда доступна только игроку-модератору.");
            return;
        }
        if (args.length < 2) {
            messageService.send(sender, "&eИспользование: /" + label + " start <игрок> [секунды]");
            return;
        }
        Player suspect = Bukkit.getPlayerExact(args[1]);
        if (suspect == null) {
            messageService.send(sender, "&cИгрок не найден онлайн.");
            return;
        }
        long duration = args.length >= 3 ? parsePositiveLong(args[2], settingsProvider.getSettings().getDefaultDurationSeconds()) : settingsProvider.getSettings().getDefaultDurationSeconds();
        checkSessionService.startCheck(moderator, suspect, duration);
    }

    private void handleRelease(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player moderator)) {
            messageService.send(sender, "&cКоманда доступна только игроку-модератору.");
            return;
        }
        if (args.length < 2) {
            messageService.send(sender, "&eИспользование: /" + label + " release <игрок>");
            return;
        }
        Player suspect = Bukkit.getPlayerExact(args[1]);
        if (suspect == null) {
            messageService.send(sender, "&cИгрок не найден онлайн.");
            return;
        }
        checkSessionService.release(moderator, suspect);
    }

    private void handleBan(CommandSender sender, String label, String[] args, CheckEndReason reasonType) {
        if (!(sender instanceof Player moderator)) {
            messageService.send(sender, "&cКоманда доступна только игроку-модератору.");
            return;
        }
        if (args.length < 2) {
            messageService.send(sender, "&eИспользование: /" + label + " " + args[0].toLowerCase(Locale.ROOT) + " <игрок> [причина]");
            return;
        }
        Player suspect = Bukkit.getPlayerExact(args[1]);
        if (suspect == null) {
            messageService.send(sender, "&cИгрок не найден онлайн.");
            return;
        }
        String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : defaultReason(reasonType);
        checkSessionService.ban(moderator, suspect, reason, reasonType);
    }

    private void handleDeny(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player moderator)) {
            messageService.send(sender, "&cКоманда доступна только игроку-модератору.");
            return;
        }
        if (args.length < 2) {
            messageService.send(sender, "&eИспользование: /" + label + " deny <игрок> [причина]");
            return;
        }
        Player suspect = Bukkit.getPlayerExact(args[1]);
        if (suspect == null) {
            messageService.send(sender, "&cИгрок не найден онлайн.");
            return;
        }
        String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Игнор проверки";
        checkSessionService.ban(moderator, suspect, reason, CheckEndReason.TIMEOUT);
    }

    private void handleAddTime(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player moderator)) {
            messageService.send(sender, "&cКоманда доступна только игроку-модератору.");
            return;
        }
        if (args.length < 3) {
            messageService.send(sender, "&eИспользование: /" + label + " addtime <игрок> <минуты>");
            return;
        }
        Player suspect = Bukkit.getPlayerExact(args[1]);
        if (suspect == null) {
            messageService.send(sender, "&cИгрок не найден онлайн.");
            return;
        }
        long minutes = parsePositiveLong(args[2], 1L);
        checkSessionService.addTime(moderator, suspect, minutes * 60L);
    }

    private void handleSetCabin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "&cКоманда доступна только игроку.");
            return;
        }
        if (!sender.hasPermission("akycheatcheck.reload")) {
            messageService.send(sender, "&cНедостаточно прав.");
            return;
        }
        String id = args.length >= 2 ? args[1] : "cabin-" + System.currentTimeMillis();
        org.bukkit.Location location = player.getLocation();
        java.util.Map<String, Object> cabin = new java.util.LinkedHashMap<>();
        cabin.put("id", id);
        cabin.put("world", location.getWorld() == null ? "world" : location.getWorld().getName());
        cabin.put("x", location.getX());
        cabin.put("y", location.getY());
        cabin.put("z", location.getZ());
        cabin.put("yaw", location.getYaw());
        cabin.put("pitch", location.getPitch());
        List<java.util.Map<?, ?>> cabins = new ArrayList<>();
        for (Object value : plugin.getConfig().getList("cabins", List.of())) {
            if (value instanceof java.util.Map<?, ?> map && !id.equals(String.valueOf(map.get("id")))) {
                cabins.add(map);
            }
        }
        cabins.add(cabin);
        plugin.getConfig().set("cabins", cabins);
        plugin.saveConfig();
        settingsProvider.reload();
        messageService.send(sender, "&aКабинка сохранена: &f" + id);
    }

    private void handleInfo(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            messageService.send(sender, "&eИспользование: /" + label + " info <игрок>");
            return;
        }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) {
            messageService.send(sender, "&cИгрок не найден онлайн.");
            return;
        }
        Optional<CheckSession> session = checkSessionService.findByParticipant(player.getUniqueId());
        messageService.send(sender, "&7Игрок: &f" + player.getName());
        messageService.send(sender, "&7Клиент: &f" + clientBrandService.describeClient(player.getUniqueId()));
        if (session.isPresent()) {
            CheckSession value = session.get();
            messageService.send(sender, "&7Проверка: &aда &7| Модератор: &f" + value.getModeratorName() + " &7| Кабинка: &f" + value.getCabin().id());
        } else {
            messageService.send(sender, "&7Проверка: &cнет");
        }
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                messageService.send(sender, "&cДля просмотра общей статистики по имени модератор должен быть онлайн.");
                return;
            }
            UUID moderatorId = target.getUniqueId();
            statisticsService.findTotal(moderatorId).thenAccept(stats -> Bukkit.getScheduler().runTask(plugin, () -> sendTotalStats(sender, target.getName(), stats)));
            return;
        }
        statisticsService.findTodayLeaderboard().thenAccept(stats -> Bukkit.getScheduler().runTask(plugin, () -> sendTodayStats(sender, stats)));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("akycheatcheck.reload")) {
            messageService.send(sender, "&cНедостаточно прав для перезагрузки.");
            return;
        }
        settingsProvider.reload();
        messageService.send(sender, "&aКонфигурация перезагружена.");
    }

    private void handleList(CommandSender sender) {
        if (checkSessionService.getSessions().isEmpty()) {
            messageService.send(sender, "&eАктивных проверок нет.");
            return;
        }
        messageService.send(sender, "&eАктивные проверки:");
        for (CheckSession session : checkSessionService.getSessions()) {
            messageService.send(sender, "&7- &f" + session.getSuspectName() + " &7проверяет &f" + session.getModeratorName() + " &7в кабинке &f" + session.getCabin().id());
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        messageService.send(sender, "&eКоманды AkyCheatCheck:");
        messageService.send(sender, "&7/" + label + " start <игрок> [секунды] &f— начать проверку");
        messageService.send(sender, "&7/" + label + " release/as <игрок> &f— отпустить игрока");
        messageService.send(sender, "&7/" + label + " ban/soft <игрок> [причина] &f— забанить по итогам проверки");
        messageService.send(sender, "&7/" + label + " deny <игрок> [причина] &f— бан за игнор");
        messageService.send(sender, "&7/" + label + " addtime <игрок> <минуты> &f— добавить время");
        messageService.send(sender, "&7/" + label + " setcabin [id] &f— сохранить кабинку на текущем месте");
        messageService.send(sender, "&7/" + label + " confess <игрок> &f— бан за признание");
        messageService.send(sender, "&7/" + label + " info <игрок> &f— клиент и статус проверки");
        messageService.send(sender, "&7/" + label + " stats [модератор] &f— статистика");
        messageService.send(sender, "&7/" + label + " list &f— активные проверки");
    }

    private void sendTotalStats(CommandSender sender, String name, List<ModeratorStatistics> stats) {
        int checks = stats.stream().mapToInt(ModeratorStatistics::getChecks).sum();
        int bans = stats.stream().mapToInt(ModeratorStatistics::getBans).sum();
        int releases = stats.stream().mapToInt(ModeratorStatistics::getReleases).sum();
        messageService.send(sender, "&eСтатистика &f" + (name == null ? "unknown" : name));
        messageService.send(sender, "&7Всего: проверки &f" + checks + "&7, баны &f" + bans + "&7, оправдания &f" + releases);
        stats.stream().limit(7).forEach(stat -> messageService.send(sender,
                "&7" + stat.getDay() + ": &f" + stat.getChecks() + " &7проверок, &c" + stat.getBans() + " &7банов, &a" + stat.getReleases() + " &7оправданий"));
    }

    private void sendTodayStats(CommandSender sender, List<ModeratorStatistics> stats) {
        messageService.send(sender, "&eСтатистика модераторов за сегодня:");
        if (stats.isEmpty()) {
            messageService.send(sender, "&7Сегодня записей нет.");
            return;
        }
        stats.stream().limit(10).forEach(stat -> messageService.send(sender,
                "&7- &f" + stat.getModeratorName() + "&7: проверки &f" + stat.getChecks() + "&7, баны &c" + stat.getBans() + "&7, оправдания &a" + stat.getReleases()));
    }

    private String defaultReason(CheckEndReason reasonType) {
        return reasonType == CheckEndReason.CONFESSION ? "Признание в читах" : "Читы";
    }

    private long parsePositiveLong(String value, long fallback) {
        try {
            return Math.max(1L, Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private List<String> filter(List<String> source, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : source) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(value);
            }
        }
        return result;
    }
}
