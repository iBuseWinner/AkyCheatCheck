package ru.akydevv.akycheatcheck.service;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ru.akydevv.akycheatcheck.config.SettingsProvider;

public final class BanAnimationService {
    private final SettingsProvider settingsProvider;

    public BanAnimationService(SettingsProvider settingsProvider) {
        this.settingsProvider = settingsProvider;
    }

    public void play(Player player) {
        if (!settingsProvider.getSettings().isBanAnimationEnabled()) {
            return;
        }
        Location location = player.getLocation();
        if (settingsProvider.getSettings().isBanAnimationLightning()) {
            player.getWorld().strikeLightningEffect(location);
        }
        if (settingsProvider.getSettings().isBanAnimationExplosionParticles()) {
            player.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, location, 1);
            player.getWorld().spawnParticle(Particle.FLAME, location, 80, 1.0D, 1.0D, 1.0D, 0.03D);
        }
        Sound sound = parseSound(settingsProvider.getSettings().getBanAnimationSound());
        if (sound != null) {
            player.getWorld().playSound(location, sound, 1.0F, 0.7F);
        }
    }

    private Sound parseSound(String name) {
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
