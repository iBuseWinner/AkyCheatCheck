package ru.akydevv.akycheatcheck.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.akydevv.akycheatcheck.model.CheckSession;
import ru.akydevv.akycheatcheck.service.CheckSessionService;

import java.util.Optional;

public final class MovementRestrictionListener implements Listener {
    private final CheckSessionService checkSessionService;

    public MovementRestrictionListener(CheckSessionService checkSessionService) {
        this.checkSessionService = checkSessionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!checkSessionService.isSuspect(player.getUniqueId())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || sameBlock(from, to)) {
            return;
        }
        Optional<CheckSession> session = checkSessionService.findBySuspect(player.getUniqueId());
        if (session.isEmpty()) {
            return;
        }
        Location locked = session.get().getCabin().toLocation().orElse(from).clone();
        locked.setYaw(to.getYaw());
        locked.setPitch(to.getPitch());
        event.setTo(locked);
    }

    private boolean sameBlock(Location first, Location second) {
        return first.getWorld() == second.getWorld()
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}
