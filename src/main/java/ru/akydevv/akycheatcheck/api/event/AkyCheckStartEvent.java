package ru.akydevv.akycheatcheck.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.akydevv.akycheatcheck.model.CheckCabin;

public final class AkyCheckStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player moderator;
    private final Player suspect;
    private final CheckCabin cabin;
    private final long durationSeconds;

    public AkyCheckStartEvent(Player moderator, Player suspect, CheckCabin cabin, long durationSeconds) {
        this.moderator = moderator;
        this.suspect = suspect;
        this.cabin = cabin;
        this.durationSeconds = durationSeconds;
    }

    public Player getModerator() {
        return moderator;
    }

    public Player getSuspect() {
        return suspect;
    }

    public CheckCabin getCabin() {
        return cabin;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
