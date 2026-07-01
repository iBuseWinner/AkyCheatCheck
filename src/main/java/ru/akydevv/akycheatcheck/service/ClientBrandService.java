package ru.akydevv.akycheatcheck.service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ClientBrandService {
    private final ConcurrentMap<UUID, String> brands = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Set<String>> channels = new ConcurrentHashMap<>();

    public void setBrand(UUID playerId, String brand) {
        brands.put(playerId, normalizeBrand(brand));
    }

    public Optional<String> getBrand(UUID playerId) {
        return Optional.ofNullable(brands.get(playerId));
    }

    public void registerChannel(UUID playerId, String channel) {
        channels.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet()).add(channel.toLowerCase());
    }

    public Set<String> getChannels(UUID playerId) {
        return Set.copyOf(channels.getOrDefault(playerId, Set.of()));
    }

    public String describeClient(UUID playerId) {
        String brand = brands.getOrDefault(playerId, "unknown");
        Set<String> playerChannels = channels.getOrDefault(playerId, Set.of());
        if (contains(playerChannels, "lunar")) {
            return "Lunar Client (brand: " + brand + ")";
        }
        if (contains(playerChannels, "badlion") || contains(playerChannels, "bml")) {
            return "Badlion Client (brand: " + brand + ")";
        }
        if (brand.contains("fabric")) {
            return "Fabric (brand: " + brand + ")";
        }
        if (brand.contains("forge") || brand.contains("fml")) {
            return "Forge (brand: " + brand + ")";
        }
        if (brand.contains("paper") || brand.contains("spigot") || brand.contains("vanilla")) {
            return "Vanilla/Server brand: " + brand;
        }
        return brand;
    }

    public void remove(UUID playerId) {
        brands.remove(playerId);
        channels.remove(playerId);
    }

    public void clear() {
        brands.clear();
        channels.clear();
    }

    private boolean contains(Set<String> values, String needle) {
        for (String value : values) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeBrand(String rawBrand) {
        String normalized = rawBrand == null ? "unknown" : rawBrand.strip().toLowerCase();
        if (normalized.length() > 64) {
            return normalized.substring(0, 64);
        }
        return normalized.isBlank() ? "unknown" : normalized;
    }
}
