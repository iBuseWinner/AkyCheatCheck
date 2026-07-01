package ru.akydevv.akycheatcheck;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.akydevv.akycheatcheck.command.CheatCheckCommand;
import ru.akydevv.akycheatcheck.config.SettingsProvider;
import ru.akydevv.akycheatcheck.config.YamlSettingsProvider;
import ru.akydevv.akycheatcheck.placeholder.AkyCheatCheckPlaceholderExpansion;
import ru.akydevv.akycheatcheck.listener.ChatIsolationListener;
import ru.akydevv.akycheatcheck.listener.ClientBrandListener;
import ru.akydevv.akycheatcheck.listener.ExploitProtectionListener;
import ru.akydevv.akycheatcheck.listener.MovementRestrictionListener;
import ru.akydevv.akycheatcheck.listener.PlayerActivityListener;
import ru.akydevv.akycheatcheck.listener.SessionDisconnectListener;
import ru.akydevv.akycheatcheck.listener.SessionInteractionRestrictionListener;
import ru.akydevv.akycheatcheck.repository.ModeratorStatisticsRepository;
import ru.akydevv.akycheatcheck.repository.YamlModeratorStatisticsRepository;
import ru.akydevv.akycheatcheck.service.AfkActivityService;
import ru.akydevv.akycheatcheck.service.AnticheatCompatibilityService;
import ru.akydevv.akycheatcheck.service.BanAnimationService;
import ru.akydevv.akycheatcheck.service.CheckAuditService;
import ru.akydevv.akycheatcheck.service.CheckSessionService;
import ru.akydevv.akycheatcheck.service.ClientBrandService;
import ru.akydevv.akycheatcheck.service.MessageService;
import ru.akydevv.akycheatcheck.service.ModeratorStatisticsService;
import ru.akydevv.akycheatcheck.service.PunishmentService;
import ru.akydevv.akycheatcheck.service.TrophyService;
import ru.akydevv.akycheatcheck.service.WebDashboardService;

import java.time.ZoneId;

public final class AkyCheatCheckPlugin extends JavaPlugin {
    private ModeratorStatisticsRepository statisticsRepository;
    private ModeratorStatisticsService statisticsService;
    private CheckSessionService checkSessionService;
    private CheckAuditService checkAuditService;
    private WebDashboardService webDashboardService;
    private AfkActivityService afkActivityService;
    private AnticheatCompatibilityService compatibilityService;
    private ClientBrandService clientBrandService;
    private ClientBrandListener clientBrandListener;

    @Override
    public void onEnable() {
        SettingsProvider settingsProvider = new YamlSettingsProvider(this);
        MessageService messageService = new MessageService(settingsProvider);
        clientBrandService = new ClientBrandService();
        statisticsRepository = new YamlModeratorStatisticsRepository(this);
        statisticsService = new ModeratorStatisticsService(statisticsRepository, ZoneId.systemDefault());
        compatibilityService = new AnticheatCompatibilityService(settingsProvider);
        checkAuditService = new CheckAuditService(this, settingsProvider);
        TrophyService trophyService = new TrophyService(settingsProvider, messageService);
        BanAnimationService banAnimationService = new BanAnimationService(settingsProvider);
        PunishmentService punishmentService = new PunishmentService(settingsProvider, messageService, banAnimationService);
        checkSessionService = new CheckSessionService(
                this,
                settingsProvider,
                messageService,
                punishmentService,
                trophyService,
                statisticsService,
                compatibilityService,
                clientBrandService,
                checkAuditService
        );
        afkActivityService = new AfkActivityService(this, settingsProvider, messageService, checkSessionService);
        webDashboardService = new WebDashboardService(this, settingsProvider, checkSessionService);

        registerCommands(settingsProvider, messageService);
        registerListeners(settingsProvider, messageService);
        registerPluginMessaging();
        registerPlaceholderExpansion();

        checkSessionService.startTimer();
        Bukkit.getOnlinePlayers().forEach(afkActivityService::markActivity);
        afkActivityService.start();
        webDashboardService.start();
        getLogger().info("AkyCheatCheck включён.");
    }

    @Override
    public void onDisable() {
        if (checkSessionService != null) {
            checkSessionService.shutdown();
        }
        if (afkActivityService != null) {
            afkActivityService.clear();
        }
        if (webDashboardService != null) {
            webDashboardService.stop();
        }
        if (statisticsService != null) {
            statisticsService.flushCache();
        }
        if (statisticsRepository != null) {
            statisticsRepository.close();
        }
        if (checkAuditService != null) {
            checkAuditService.close();
        }
        if (compatibilityService != null) {
            compatibilityService.clearMemory();
        }
        if (clientBrandService != null) {
            clientBrandService.clear();
        }
        Bukkit.getScheduler().cancelTasks(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getLogger().info("AkyCheatCheck выключен.");
    }

    private void registerCommands(SettingsProvider settingsProvider, MessageService messageService) {
        CheatCheckCommand executor = new CheatCheckCommand(
                this,
                settingsProvider,
                checkSessionService,
                statisticsService,
                clientBrandService,
                messageService
        );
        PluginCommand command = getCommand("akycheck");
        if (command == null) {
            throw new IllegalStateException("Команда akycheck отсутствует в plugin.yml");
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerListeners(SettingsProvider settingsProvider, MessageService messageService) {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new MovementRestrictionListener(checkSessionService), this);
        pluginManager.registerEvents(new ChatIsolationListener(this, settingsProvider, checkSessionService, messageService), this);
        pluginManager.registerEvents(new ExploitProtectionListener(checkSessionService, messageService), this);
        pluginManager.registerEvents(new SessionInteractionRestrictionListener(checkSessionService, messageService), this);
        pluginManager.registerEvents(new PlayerActivityListener(afkActivityService), this);
        pluginManager.registerEvents(new SessionDisconnectListener(checkSessionService, afkActivityService), this);
        clientBrandListener = new ClientBrandListener(clientBrandService);
        pluginManager.registerEvents(clientBrandListener, this);
    }

    private void registerPlaceholderExpansion() {
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("PlaceholderAPI не найден: placeholders отключены.");
            return;
        }
        new AkyCheatCheckPlaceholderExpansion(checkSessionService, statisticsService).register();
        getLogger().info("PlaceholderAPI expansion зарегистрирован: %akycheck_active_checks%, %akycheck_moderator_checks%, %akycheck_moderator_bans%.");
    }

    private void registerPluginMessaging() {
        getServer().getMessenger().registerIncomingPluginChannel(this, "minecraft:brand", clientBrandListener);
    }
}
