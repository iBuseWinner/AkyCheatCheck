package ru.akydevv.akycheatcheck.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.akydevv.akycheatcheck.service.AfkActivityService;

public final class PlayerActivityListener implements Listener {
    private final AfkActivityService afkActivityService;

    public PlayerActivityListener(AfkActivityService afkActivityService) {
        this.afkActivityService = afkActivityService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        afkActivityService.markActivity(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() != null && !sameBlock(event.getFrom(), event.getTo())) {
            afkActivityService.markActivity(event.getPlayer());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        afkActivityService.markActivity(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        afkActivityService.markActivity(event.getPlayer());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        afkActivityService.markActivity(event.getPlayer());
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        afkActivityService.markActivity(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player player) {
            afkActivityService.markActivity(player);
        }
    }

    private boolean sameBlock(org.bukkit.Location first, org.bukkit.Location second) {
        return first.getWorld() == second.getWorld()
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}
