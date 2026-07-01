package ru.akydevv.akycheatcheck.config;

public interface SettingsProvider {
    PluginSettings getSettings();
    void reload();
}
