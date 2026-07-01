package ru.akydevv.akycheatcheck.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.Optional;

public record CheckCabin(String id, String worldName, double x, double y, double z, float yaw, float pitch) {

    public CheckCabin {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(worldName, "worldName");
    }

    public Optional<Location> toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, x, y, z, yaw, pitch));
    }
}
