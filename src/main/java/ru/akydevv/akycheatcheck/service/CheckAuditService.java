package ru.akydevv.akycheatcheck.service;

import org.bukkit.plugin.Plugin;
import ru.akydevv.akycheatcheck.config.SettingsProvider;
import ru.akydevv.akycheatcheck.model.CheckEndReason;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class CheckAuditService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Plugin plugin;
    private final SettingsProvider settingsProvider;
    private final ExecutorService executor;
    private final HttpClient httpClient;

    public CheckAuditService(Plugin plugin, SettingsProvider settingsProvider) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "AkyCheatCheck-Audit");
            thread.setDaemon(true);
            return thread;
        });
        this.httpClient = HttpClient.newBuilder().executor(executor).build();
    }

    public void logStart(String moderatorName, String suspectName, String client, String cabinId, long durationSeconds) {
        String message = "START moderator=" + moderatorName + " suspect=" + suspectName + " client=" + client + " cabin=" + cabinId + " duration=" + durationSeconds;
        submit(message);
    }

    public void logEnd(String moderatorName, String suspectName, CheckEndReason reason, String punishmentReason) {
        String message = "END moderator=" + moderatorName + " suspect=" + suspectName + " result=" + reason + " reason=" + punishmentReason;
        submit(message);
    }

    public void close() {
        executor.shutdownNow();
    }

    private void submit(String message) {
        CompletableFuture.runAsync(() -> {
            String line = "[" + FORMATTER.format(LocalDateTime.now()) + "] " + message;
            writeFile(line);
            sendDiscord(line);
            sendTelegram(line);
        }, executor).exceptionally(exception -> {
            plugin.getLogger().log(Level.WARNING, "Не удалось записать аудит проверки", exception);
            return null;
        });
    }

    private void writeFile(String line) {
        if (!settingsProvider.getSettings().isAuditFileEnabled()) {
            return;
        }
        try {
            Path directory = plugin.getDataFolder().toPath().resolve("logs");
            Files.createDirectories(directory);
            Path file = directory.resolve("checks.log");
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Не удалось записать checks.log", exception);
        }
    }

    private void sendDiscord(String line) {
        String webhook = settingsProvider.getSettings().getDiscordWebhookUrl();
        if (!settingsProvider.getSettings().isDiscordWebhookEnabled() || webhook.isBlank()) {
            return;
        }
        String body = "{\"content\":\"" + escapeJson(line) + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(webhook))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    private void sendTelegram(String line) {
        String token = settingsProvider.getSettings().getTelegramBotToken();
        String chatId = settingsProvider.getSettings().getTelegramChatId();
        String apiLink = settingsProvider.getSettings().getApiLink();
        if (!settingsProvider.getSettings().isTelegramEnabled() || token.isBlank() || chatId.isBlank()) {
            return;
        }
        String body = "chat_id=" + encode(chatId) + "&text=" + encode(line);
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiLink + "/bot" + token + "/sendMessage"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
