package ru.akydevv.akycheatcheck.api.event;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.akydevv.akycheatcheck.model.CheckEndReason;

import java.util.UUID;

public final class AkyCheckEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID suspectId;
    private final String suspectName;
    private final OfflinePlayer moderator;
    private final CheckEndReason reason;
    private final String punishmentReason;

    public AkyCheckEndEvent(UUID suspectId, String suspectName, OfflinePlayer moderator, CheckEndReason reason, String punishmentReason) {
        this.suspectId = suspectId;
        this.suspectName = suspectName;
        this.moderator = moderator;
        this.reason = reason;
        this.punishmentReason = punishmentReason;
    }

    public UUID getSuspectId() {
        return suspectId;
    }

    public String getSuspectName() {
        return suspectName;
    }

    public OfflinePlayer getModerator() {
        return moderator;
    }

    public CheckEndReason getReason() {
        return reason;
    }

    public String getPunishmentReason() {
        return punishmentReason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
