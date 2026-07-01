package ru.akydevv.akycheatcheck.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.akydevv.akycheatcheck.config.SettingsProvider;

import java.util.Map;
import java.util.Objects;

public final class PunishmentService {
    private final SettingsProvider settingsProvider;
    private final MessageService messageService;
    private final BanAnimationService banAnimationService;

    public PunishmentService(SettingsProvider settingsProvider, MessageService messageService, BanAnimationService banAnimationService) {
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
        this.messageService = Objects.requireNonNull(messageService, "messageService");
        this.banAnimationService = Objects.requireNonNull(banAnimationService, "banAnimationService");
    }

    public void ban(Player player, String reason) {
        banAnimationService.play(player);
        String command = settingsProvider.getSettings().getBanCommand()
                .replace("%player%", player.getName())
                .replace("%reason%", reason)
                .replaceFirst("^/", "");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        messageService.broadcast(messageService.format(settingsProvider.getSettings().getBanBroadcast(), Map.of(
                "player", player.getName(),
                "reason", reason,
                "moderator", "system"
        )));
    }
}
