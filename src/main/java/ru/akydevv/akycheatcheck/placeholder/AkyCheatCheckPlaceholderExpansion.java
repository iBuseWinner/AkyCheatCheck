package ru.akydevv.akycheatcheck.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.akydevv.akycheatcheck.service.CheckSessionService;
import ru.akydevv.akycheatcheck.service.ModeratorStatisticsService;

import java.util.Locale;
import java.util.Objects;

public final class AkyCheatCheckPlaceholderExpansion extends PlaceholderExpansion {
    private final CheckSessionService checkSessionService;
    private final ModeratorStatisticsService moderatorStatisticsService;

    public AkyCheatCheckPlaceholderExpansion(CheckSessionService checkSessionService,
                                             ModeratorStatisticsService moderatorStatisticsService) {
        this.checkSessionService = Objects.requireNonNull(checkSessionService, "checkSessionService");
        this.moderatorStatisticsService = Objects.requireNonNull(moderatorStatisticsService, "moderatorStatisticsService");
    }

    @Override
    public @NotNull String getIdentifier() {
        return "akycheck";
    }

    @Override
    public @NotNull String getAuthor() {
        return "akydevv";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String key = params.toLowerCase(Locale.ROOT);
        if (key.equals("active_checks")) {
            return String.valueOf(checkSessionService.getActiveSessionCount());
        }
        if (player == null) {
            return "0";
        }
        if (key.equals("moderator_checks")) {
            return String.valueOf(moderatorStatisticsService.getCachedTotalChecks(player.getUniqueId()));
        }
        if (key.equals("moderator_bans")) {
            return String.valueOf(moderatorStatisticsService.getCachedTotalBans(player.getUniqueId()));
        }
        return null;
    }
}
