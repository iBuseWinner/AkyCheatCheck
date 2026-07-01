package ru.akydevv.akycheatcheck.model;

import org.bukkit.Location;
import org.bukkit.boss.BossBar;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class CheckSession {
    private final UUID suspectId;
    private final UUID moderatorId;
    private final String suspectName;
    private final String moderatorName;
    private final CheckCabin cabin;
    private final Location returnLocation;
    private final Instant startedAt;
    private long durationSeconds;
    private final BossBar bossBar;

    public CheckSession(UUID suspectId,
                        UUID moderatorId,
                        String suspectName,
                        String moderatorName,
                        CheckCabin cabin,
                        Location returnLocation,
                        Instant startedAt,
                        long durationSeconds,
                        BossBar bossBar) {
        this.suspectId = Objects.requireNonNull(suspectId, "suspectId");
        this.moderatorId = Objects.requireNonNull(moderatorId, "moderatorId");
        this.suspectName = Objects.requireNonNull(suspectName, "suspectName");
        this.moderatorName = Objects.requireNonNull(moderatorName, "moderatorName");
        this.cabin = Objects.requireNonNull(cabin, "cabin");
        this.returnLocation = Objects.requireNonNull(returnLocation, "returnLocation").clone();
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.durationSeconds = durationSeconds;
        this.bossBar = Objects.requireNonNull(bossBar, "bossBar");
    }

    public UUID getSuspectId() {
        return suspectId;
    }

    public UUID getModeratorId() {
        return moderatorId;
    }

    public String getSuspectName() {
        return suspectName;
    }

    public String getModeratorName() {
        return moderatorName;
    }

    public CheckCabin getCabin() {
        return cabin;
    }

    public Location getReturnLocation() {
        return returnLocation.clone();
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public void addDurationSeconds(long seconds) {
        if (seconds > 0L) {
            durationSeconds += seconds;
        }
    }

    public long elapsedSeconds(Instant now) {
        return Math.max(0L, now.getEpochSecond() - startedAt.getEpochSecond());
    }

    public long remainingSeconds(Instant now) {
        return Math.max(0L, durationSeconds - elapsedSeconds(now));
    }
}
