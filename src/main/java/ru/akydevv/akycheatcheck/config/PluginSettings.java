package ru.akydevv.akycheatcheck.config;

import ru.akydevv.akycheatcheck.model.CheckCabin;

import java.util.List;
import java.util.Objects;

public final class PluginSettings {
    private final String prefix;
    private final long defaultDurationSeconds;
    private final long maxDurationSeconds;
    private final boolean autoBanOnTimeout;
    private final String banCommand;
    private final List<String> confessionPhrases;
    private final List<String> allowedCommands;
    private final String blockedCommandMessage;
    private final String bossBarTitle;
    private final String releaseBroadcast;
    private final String banBroadcast;
    private final boolean trophyEnabled;
    private final String trophyMaterial;
    private final String trophyName;
    private final List<String> trophyLore;
    private final List<CheckCabin> cabins;
    private final boolean afkEnabled;
    private final long afkIdleSecondsBeforeCheck;
    private final long afkScanPeriodSeconds;
    private final boolean enableFlightWhileFrozen;
    private final boolean disablePaperFlyKick;
    private final List<String> commandsOnCheckStart;
    private final List<String> commandsOnCheckEnd;
    private final boolean banAnimationEnabled;
    private final boolean banAnimationLightning;
    private final boolean banAnimationExplosionParticles;
    private final String banAnimationSound;
    private final boolean auditFileEnabled;
    private final boolean discordWebhookEnabled;
    private final String discordWebhookUrl;
    private final boolean telegramEnabled;
    private final String telegramBotToken;
    private final String telegramChatId;
    private final String telegramApiLink;
    private final boolean webDashboardEnabled;
    private final String webDashboardHost;
    private final int webDashboardPort;
    private final String webDashboardToken;

    public PluginSettings(String prefix,
                          long defaultDurationSeconds,
                          long maxDurationSeconds,
                          boolean autoBanOnTimeout,
                          String banCommand,
                          List<String> confessionPhrases,
                          List<String> allowedCommands,
                          String blockedCommandMessage,
                          String bossBarTitle,
                          String releaseBroadcast,
                          String banBroadcast,
                          boolean trophyEnabled,
                          String trophyMaterial,
                          String trophyName,
                          List<String> trophyLore,
                          List<CheckCabin> cabins,
                          boolean afkEnabled,
                          long afkIdleSecondsBeforeCheck,
                          long afkScanPeriodSeconds,
                          boolean enableFlightWhileFrozen,
                          boolean disablePaperFlyKick,
                          List<String> commandsOnCheckStart,
                          List<String> commandsOnCheckEnd,
                          boolean banAnimationEnabled,
                          boolean banAnimationLightning,
                          boolean banAnimationExplosionParticles,
                          String banAnimationSound,
                          boolean auditFileEnabled,
                          boolean discordWebhookEnabled,
                          String discordWebhookUrl,
                          boolean telegramEnabled,
                          String telegramBotToken,
                          String telegramChatId,
                          String telegramApiLink,
                          boolean webDashboardEnabled,
                          String webDashboardHost,
                          int webDashboardPort,
                          String webDashboardToken) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
        this.defaultDurationSeconds = Math.max(1L, defaultDurationSeconds);
        this.maxDurationSeconds = Math.max(this.defaultDurationSeconds, maxDurationSeconds);
        this.autoBanOnTimeout = autoBanOnTimeout;
        this.banCommand = Objects.requireNonNull(banCommand, "banCommand");
        this.confessionPhrases = List.copyOf(confessionPhrases);
        this.allowedCommands = List.copyOf(allowedCommands);
        this.blockedCommandMessage = Objects.requireNonNull(blockedCommandMessage, "blockedCommandMessage");
        this.bossBarTitle = Objects.requireNonNull(bossBarTitle, "bossBarTitle");
        this.releaseBroadcast = Objects.requireNonNull(releaseBroadcast, "releaseBroadcast");
        this.banBroadcast = Objects.requireNonNull(banBroadcast, "banBroadcast");
        this.trophyEnabled = trophyEnabled;
        this.trophyMaterial = Objects.requireNonNull(trophyMaterial, "trophyMaterial");
        this.trophyName = Objects.requireNonNull(trophyName, "trophyName");
        this.trophyLore = List.copyOf(trophyLore);
        this.cabins = List.copyOf(cabins);
        this.afkEnabled = afkEnabled;
        this.afkIdleSecondsBeforeCheck = Math.max(30L, afkIdleSecondsBeforeCheck);
        this.afkScanPeriodSeconds = Math.max(5L, afkScanPeriodSeconds);
        this.enableFlightWhileFrozen = enableFlightWhileFrozen;
        this.disablePaperFlyKick = disablePaperFlyKick;
        this.commandsOnCheckStart = List.copyOf(commandsOnCheckStart);
        this.commandsOnCheckEnd = List.copyOf(commandsOnCheckEnd);
        this.banAnimationEnabled = banAnimationEnabled;
        this.banAnimationLightning = banAnimationLightning;
        this.banAnimationExplosionParticles = banAnimationExplosionParticles;
        this.banAnimationSound = Objects.requireNonNull(banAnimationSound, "banAnimationSound");
        this.auditFileEnabled = auditFileEnabled;
        this.discordWebhookEnabled = discordWebhookEnabled;
        this.discordWebhookUrl = Objects.requireNonNull(discordWebhookUrl, "discordWebhookUrl");
        this.telegramEnabled = telegramEnabled;
        this.telegramBotToken = Objects.requireNonNull(telegramBotToken, "telegramBotToken");
        this.telegramChatId = Objects.requireNonNull(telegramChatId, "telegramChatId");
        this.telegramApiLink = Objects.requireNonNull(telegramApiLink, "telegramApiLink");
        this.webDashboardEnabled = webDashboardEnabled;
        this.webDashboardHost = Objects.requireNonNull(webDashboardHost, "webDashboardHost");
        this.webDashboardPort = Math.max(1, Math.min(65535, webDashboardPort));
        this.webDashboardToken = Objects.requireNonNull(webDashboardToken, "webDashboardToken");
    }

    public String getPrefix() { return prefix; }
    public long getDefaultDurationSeconds() { return defaultDurationSeconds; }
    public long getMaxDurationSeconds() { return maxDurationSeconds; }
    public boolean isAutoBanOnTimeout() { return autoBanOnTimeout; }
    public String getBanCommand() { return banCommand; }
    public List<String> getConfessionPhrases() { return confessionPhrases; }
    public List<String> getAllowedCommands() { return allowedCommands; }
    public String getBlockedCommandMessage() { return blockedCommandMessage; }
    public String getBossBarTitle() { return bossBarTitle; }
    public String getReleaseBroadcast() { return releaseBroadcast; }
    public String getBanBroadcast() { return banBroadcast; }
    public boolean isTrophyEnabled() { return trophyEnabled; }
    public String getTrophyMaterial() { return trophyMaterial; }
    public String getTrophyName() { return trophyName; }
    public List<String> getTrophyLore() { return trophyLore; }
    public List<CheckCabin> getCabins() { return cabins; }
    public boolean isAfkEnabled() { return afkEnabled; }
    public long getAfkIdleSecondsBeforeCheck() { return afkIdleSecondsBeforeCheck; }
    public long getAfkScanPeriodSeconds() { return afkScanPeriodSeconds; }
    public boolean isEnableFlightWhileFrozen() { return enableFlightWhileFrozen; }
    public boolean isDisablePaperFlyKick() { return disablePaperFlyKick; }
    public List<String> getCommandsOnCheckStart() { return commandsOnCheckStart; }
    public List<String> getCommandsOnCheckEnd() { return commandsOnCheckEnd; }
    public boolean isBanAnimationEnabled() { return banAnimationEnabled; }
    public boolean isBanAnimationLightning() { return banAnimationLightning; }
    public boolean isBanAnimationExplosionParticles() { return banAnimationExplosionParticles; }
    public String getBanAnimationSound() { return banAnimationSound; }
    public boolean isAuditFileEnabled() { return auditFileEnabled; }
    public boolean isDiscordWebhookEnabled() { return discordWebhookEnabled; }
    public String getDiscordWebhookUrl() { return discordWebhookUrl; }
    public boolean isTelegramEnabled() { return telegramEnabled; }
    public String getTelegramBotToken() { return telegramBotToken; }
    public String getTelegramChatId() { return telegramChatId; }
    public String getApiLink() { return telegramApiLink; }
    public boolean isWebDashboardEnabled() { return webDashboardEnabled; }
    public String getWebDashboardHost() { return webDashboardHost; }
    public int getWebDashboardPort() { return webDashboardPort; }
    public String getWebDashboardToken() { return webDashboardToken; }
}
