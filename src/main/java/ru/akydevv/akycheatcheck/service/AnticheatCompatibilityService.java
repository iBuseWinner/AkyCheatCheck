package ru.akydevv.akycheatcheck.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.akydevv.akycheatcheck.config.SettingsProvider;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AnticheatCompatibilityService {
    private final SettingsProvider settingsProvider;
    private final Set<UUID> hadAllowFlight = ConcurrentHashMap.newKeySet();
    private final Set<UUID> hadFlying = ConcurrentHashMap.newKeySet();

    public AnticheatCompatibilityService(SettingsProvider settingsProvider) {
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
    }

    public void applyCheckProtection(Player player) {
        if (settingsProvider.getSettings().isEnableFlightWhileFrozen() || settingsProvider.getSettings().isDisablePaperFlyKick()) {
            if (player.getAllowFlight()) {
                hadAllowFlight.add(player.getUniqueId());
            }
            if (player.isFlying()) {
                hadFlying.add(player.getUniqueId());
            }
            player.setAllowFlight(true);
        }
        executeConfiguredCommands(settingsProvider.getSettings().getCommandsOnCheckStart(), player);
    }

    public void removeCheckProtection(Player player) {
        if (!hadAllowFlight.remove(player.getUniqueId())) {
            player.setAllowFlight(false);
        }
        if (!hadFlying.remove(player.getUniqueId()) && player.isFlying()) {
            player.setFlying(false);
        }
        executeConfiguredCommands(settingsProvider.getSettings().getCommandsOnCheckEnd(), player);
    }

    public void clearMemory() {
        hadAllowFlight.clear();
        hadFlying.clear();
    }

    private void executeConfiguredCommands(Iterable<String> commands, Player player) {
        for (String template : commands) {
            String command = template.replace("%player%", player.getName()).replaceFirst("^/", "");
            if (!command.isBlank()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }
}
