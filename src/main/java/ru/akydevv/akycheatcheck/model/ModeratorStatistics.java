package ru.akydevv.akycheatcheck.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class ModeratorStatistics {
    private final UUID moderatorId;
    private final String moderatorName;
    private final LocalDate day;
    private final int checks;
    private final int bans;
    private final int releases;

    public ModeratorStatistics(UUID moderatorId, String moderatorName, LocalDate day, int checks, int bans, int releases) {
        this.moderatorId = Objects.requireNonNull(moderatorId, "moderatorId");
        this.moderatorName = Objects.requireNonNull(moderatorName, "moderatorName");
        this.day = Objects.requireNonNull(day, "day");
        this.checks = Math.max(0, checks);
        this.bans = Math.max(0, bans);
        this.releases = Math.max(0, releases);
    }

    public UUID getModeratorId() {
        return moderatorId;
    }

    public String getModeratorName() {
        return moderatorName;
    }

    public LocalDate getDay() {
        return day;
    }

    public int getChecks() {
        return checks;
    }

    public int getBans() {
        return bans;
    }

    public int getReleases() {
        return releases;
    }

    public ModeratorStatistics withAddedCheck() {
        return new ModeratorStatistics(moderatorId, moderatorName, day, checks + 1, bans, releases);
    }

    public ModeratorStatistics withAddedBan() {
        return new ModeratorStatistics(moderatorId, moderatorName, day, checks, bans + 1, releases);
    }

    public ModeratorStatistics withAddedRelease() {
        return new ModeratorStatistics(moderatorId, moderatorName, day, checks, bans, releases + 1);
    }
}
