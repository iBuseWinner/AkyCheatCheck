package ru.akydevv.akycheatcheck.repository;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.akydevv.akycheatcheck.model.ModeratorStatistics;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class YamlModeratorStatisticsRepository implements ModeratorStatisticsRepository {
    private final JavaPlugin plugin;
    private final File file;
    private final ExecutorService executor;
    private final Object fileLock = new Object();

    public YamlModeratorStatisticsRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "moderator-statistics.yml");
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "AkyCheatCheck-Statistics-IO");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public CompletableFuture<Optional<ModeratorStatistics>> findDaily(UUID moderatorId, LocalDate day) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (fileLock) {
                YamlConfiguration yaml = loadYaml();
                ConfigurationSection section = yaml.getConfigurationSection(path(moderatorId, day));
                if (section == null) {
                    return Optional.empty();
                }
                return Optional.of(read(moderatorId, day, section));
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<ModeratorStatistics>> findAllForModerator(UUID moderatorId) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (fileLock) {
                YamlConfiguration yaml = loadYaml();
                ConfigurationSection root = yaml.getConfigurationSection("moderators." + moderatorId);
                List<ModeratorStatistics> result = new ArrayList<>();
                if (root == null) {
                    return result;
                }
                for (String dayKey : root.getKeys(false)) {
                    try {
                        LocalDate day = LocalDate.parse(dayKey);
                        ConfigurationSection section = root.getConfigurationSection(dayKey);
                        if (section != null) {
                            result.add(read(moderatorId, day, section));
                        }
                    } catch (RuntimeException ignored) {
                        plugin.getLogger().warning("Некорректная дата в статистике: " + dayKey);
                    }
                }
                return result;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<ModeratorStatistics>> findAllDaily(LocalDate day) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (fileLock) {
                YamlConfiguration yaml = loadYaml();
                ConfigurationSection root = yaml.getConfigurationSection("moderators");
                List<ModeratorStatistics> result = new ArrayList<>();
                if (root == null) {
                    return result;
                }
                for (String moderatorKey : root.getKeys(false)) {
                    try {
                        UUID moderatorId = UUID.fromString(moderatorKey);
                        ConfigurationSection section = yaml.getConfigurationSection(path(moderatorId, day));
                        if (section != null) {
                            result.add(read(moderatorId, day, section));
                        }
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("Некорректный UUID модератора в статистике: " + moderatorKey);
                    }
                }
                return result;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> save(ModeratorStatistics statistics) {
        return CompletableFuture.runAsync(() -> {
            synchronized (fileLock) {
                YamlConfiguration yaml = loadYaml();
                String path = path(statistics.getModeratorId(), statistics.getDay());
                yaml.set(path + ".name", statistics.getModeratorName());
                yaml.set(path + ".checks", statistics.getChecks());
                yaml.set(path + ".bans", statistics.getBans());
                yaml.set(path + ".releases", statistics.getReleases());
                saveYaml(yaml);
            }
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private ModeratorStatistics read(UUID moderatorId, LocalDate day, ConfigurationSection section) {
        return new ModeratorStatistics(
                moderatorId,
                section.getString("name", "unknown"),
                day,
                section.getInt("checks", 0),
                section.getInt("bans", 0),
                section.getInt("releases", 0)
        );
    }

    private String path(UUID moderatorId, LocalDate day) {
        return "moderators." + moderatorId + "." + day;
    }

    private YamlConfiguration loadYaml() {
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Не удалось создать папку данных: " + parent.getAbsolutePath());
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void saveYaml(YamlConfiguration yaml) {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить статистику модераторов", exception);
        }
    }
}
