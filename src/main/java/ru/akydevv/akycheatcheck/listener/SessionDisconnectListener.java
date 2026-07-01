package ru.akydevv.akycheatcheck.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.akydevv.akycheatcheck.model.CheckEndReason;
import ru.akydevv.akycheatcheck.service.AfkActivityService;
import ru.akydevv.akycheatcheck.service.CheckSessionService;

public final class SessionDisconnectListener implements Listener {
    private final CheckSessionService checkSessionService;
    private final AfkActivityService afkActivityService;

    public SessionDisconnectListener(CheckSessionService checkSessionService, AfkActivityService afkActivityService) {
        this.checkSessionService = checkSessionService;
        this.afkActivityService = afkActivityService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (checkSessionService.isSuspect(event.getPlayer().getUniqueId())) {
            checkSessionService.banBySystem(event.getPlayer().getUniqueId(), CheckEndReason.DISCONNECT, "Выход во время проверки");
        }
        afkActivityService.forget(event.getPlayer());
    }
}
