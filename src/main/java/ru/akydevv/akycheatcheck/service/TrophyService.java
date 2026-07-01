package ru.akydevv.akycheatcheck.service;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.akydevv.akycheatcheck.config.SettingsProvider;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TrophyService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final SettingsProvider settingsProvider;
    private final MessageService messageService;

    public TrophyService(SettingsProvider settingsProvider, MessageService messageService) {
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
        this.messageService = Objects.requireNonNull(messageService, "messageService");
    }

    public void giveTrophy(Player player, String moderatorName) {
        if (!settingsProvider.getSettings().isTrophyEnabled()) {
            return;
        }
        Material material = Material.matchMaterial(settingsProvider.getSettings().getTrophyMaterial());
        if (material == null || material.isAir()) {
            material = Material.DIAMOND;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = Map.of(
                    "player", player.getName(),
                    "moderator", moderatorName,
                    "date", LocalDateTime.now().format(DATE_FORMAT)
            );
            meta.setDisplayName(messageService.format(settingsProvider.getSettings().getTrophyName(), placeholders));
            List<String> lore = settingsProvider.getSettings().getTrophyLore().stream()
                    .map(line -> messageService.format(line, placeholders))
                    .toList();
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
