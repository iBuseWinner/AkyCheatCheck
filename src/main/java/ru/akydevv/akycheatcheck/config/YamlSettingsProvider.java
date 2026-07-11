package ru.akydevv.akycheatcheck.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import ru.akydevv.akycheatcheck.model.CheckCabin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class YamlSettingsProvider implements SettingsProvider {
    private final JavaPlugin plugin;
    private volatile PluginSettings settings;

    public YamlSettingsProvider(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        reload();
    }

    @Override
    public PluginSettings getSettings() {
        return settings;
    }

    @Override
    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        List<CheckCabin> cabins = new ArrayList<>();
        List<?> rawCabins = plugin.getConfig().getList("cabins", List.of());
        for (Object rawCabin : rawCabins) {
            if (rawCabin instanceof java.util.Map<?, ?> map) {
                String id = String.valueOf(mapValue(map, "id", "default"));
                String world = String.valueOf(mapValue(map, "world", "world"));
                double x = toDouble(map.get("x"), 0.5D);
                double y = toDouble(map.get("y"), 100.0D);
                double z = toDouble(map.get("z"), 0.5D);
                float yaw = (float) toDouble(map.get("yaw"), 0.0D);
                float pitch = (float) toDouble(map.get("pitch"), 0.0D);
                cabins.add(new CheckCabin(id, world, x, y, z, yaw, pitch));
            }
        }
        if (cabins.isEmpty()) {
            cabins.add(new CheckCabin("default", "world", 0.5D, 100.0D, 0.5D, 0.0F, 0.0F));
        }

        settings = new PluginSettings(
                plugin.getConfig().getString("language.prefix", "&8[&cAkyCheck&8] &r"),
                plugin.getConfig().getLong("check.default-duration-seconds", 300L),
                plugin.getConfig().getLong("check.max-duration-seconds", 1800L),
                plugin.getConfig().getBoolean("check.auto-ban-on-timeout", true),
                plugin.getConfig().getString("check.ban-command", "ban %player% %reason%"),
                plugin.getConfig().getStringList("check.confession-phrases"),
                plugin.getConfig().getStringList("check.allowed-commands"),
                plugin.getConfig().getString("check.blocked-command-message", "&cКоманды заблокированы."),
                plugin.getConfig().getString("check.bossbar-title", "&cПроверка &f%time%"),
                plugin.getConfig().getString("check.release-broadcast", "&a%player% прошёл проверку."),
                plugin.getConfig().getString("check.ban-broadcast", "&c%player% забанен: %reason%"),
                plugin.getConfig().getBoolean("check.trophy.enabled", true),
                plugin.getConfig().getString("check.trophy.material", "DIAMOND"),
                plugin.getConfig().getString("check.trophy.name", "&bТрофей честного игрока"),
                plugin.getConfig().getStringList("check.trophy.lore"),
                cabins,
                plugin.getConfig().getBoolean("afk.enabled", true),
                plugin.getConfig().getLong("afk.idle-seconds-before-check", 600L),
                plugin.getConfig().getLong("afk.scan-period-seconds", 30L),
                plugin.getConfig().getBoolean("compatibility.enable-flight-while-frozen", true),
                plugin.getConfig().getBoolean("compatibility.disable-paper-fly-kick", true),
                plugin.getConfig().getStringList("compatibility.commands-on-check-start"),
                plugin.getConfig().getStringList("compatibility.commands-on-check-end"),
                plugin.getConfig().getBoolean("ban-animation.enabled", true),
                plugin.getConfig().getBoolean("ban-animation.lightning", true),
                plugin.getConfig().getBoolean("ban-animation.explosion-particles", true),
                plugin.getConfig().getString("ban-animation.sound", "ENTITY_WITHER_SPAWN"),
                plugin.getConfig().getBoolean("audit.file.enabled", true),
                plugin.getConfig().getBoolean("audit.discord.enabled", false),
                plugin.getConfig().getString("audit.discord.webhook-url", ""),
                plugin.getConfig().getBoolean("audit.telegram.enabled", false),
                plugin.getConfig().getString("audit.telegram.bot-token", ""),
                plugin.getConfig().getString("audit.telegram.chat-id", ""),
                plugin.getConfig().getString("audit.telegram.api-link", "https://api.telegram.org"),
                plugin.getConfig().getBoolean("web-dashboard.enabled", true),
                plugin.getConfig().getString("web-dashboard.host", "0.0.0.0"),
                plugin.getConfig().getInt("web-dashboard.port", 8094),
                plugin.getConfig().getString("web-dashboard.token", "change-me")
        );
    }

    private Object mapValue(java.util.Map<?, ?> map, String key, Object fallback) {
        Object value = map.get(key);
        return value == null ? fallback : value;
    }

    private double toDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
