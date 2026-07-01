package ru.akydevv.akycheatcheck.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import ru.akydevv.akycheatcheck.config.SettingsProvider;
import ru.akydevv.akycheatcheck.model.CheckEndReason;
import ru.akydevv.akycheatcheck.model.CheckSession;
import ru.akydevv.akycheatcheck.service.CheckSessionService;
import ru.akydevv.akycheatcheck.service.MessageService;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ChatIsolationListener implements Listener {
    private final Plugin plugin;
    private final SettingsProvider settingsProvider;
    private final CheckSessionService checkSessionService;
    private final MessageService messageService;

    public ChatIsolationListener(Plugin plugin,
                                 SettingsProvider settingsProvider,
                                 CheckSessionService checkSessionService,
                                 MessageService messageService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
        this.checkSessionService = Objects.requireNonNull(checkSessionService, "checkSessionService");
        this.messageService = Objects.requireNonNull(messageService, "messageService");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Optional<CheckSession> sessionOptional = checkSessionService.findByParticipant(event.getPlayer().getUniqueId());
        if (sessionOptional.isEmpty()) {
            return;
        }
        CheckSession session = sessionOptional.get();
        event.getRecipients().clear();
        Player suspect = Bukkit.getPlayer(session.getSuspectId());
        Player moderator = Bukkit.getPlayer(session.getModeratorId());
        if (suspect != null) {
            event.getRecipients().add(suspect);
        }
        if (moderator != null) {
            event.getRecipients().add(moderator);
        }
        event.setFormat("§8[§cПроверка§8] §f%1$s§7: §f%2$s");

        if (event.getPlayer().getUniqueId().equals(session.getSuspectId()) && containsConfession(event.getMessage())) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    checkSessionService.banBySystem(session.getSuspectId(), CheckEndReason.CONFESSION, "Признание в читах"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!checkSessionService.isSuspect(event.getPlayer().getUniqueId())) {
            return;
        }
        String command = event.getMessage().toLowerCase(Locale.ROOT);
        boolean allowed = settingsProvider.getSettings().getAllowedCommands().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(command::startsWith);
        if (allowed) {
            return;
        }
        event.setCancelled(true);
        messageService.sendRaw(event.getPlayer(), settingsProvider.getSettings().getBlockedCommandMessage());
    }

    private boolean containsConfession(String message) {
        String normalized = message.toLowerCase(Locale.ROOT).replace('ё', 'е');
        return settingsProvider.getSettings().getConfessionPhrases().stream()
                .map(phrase -> phrase.toLowerCase(Locale.ROOT).replace('ё', 'е'))
                .anyMatch(normalized::contains);
    }
}
