package ru.akydevv.akycheatcheck.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import ru.akydevv.akycheatcheck.config.SettingsProvider;

import java.util.Map;
import java.util.Objects;

public final class MessageService {
    private final SettingsProvider settingsProvider;

    public MessageService(SettingsProvider settingsProvider) {
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(color(settingsProvider.getSettings().getPrefix() + message));
    }

    public void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    public void broadcast(String message) {
        Bukkit.broadcastMessage(color(settingsProvider.getSettings().getPrefix() + message));
    }

    public String format(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return color(result);
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
