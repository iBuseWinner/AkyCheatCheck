package ru.akydevv.akycheatcheck.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import ru.akydevv.akycheatcheck.service.ClientBrandService;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ClientBrandListener implements Listener, PluginMessageListener {
    private final ClientBrandService clientBrandService;

    public ClientBrandListener(ClientBrandService clientBrandService) {
        this.clientBrandService = Objects.requireNonNull(clientBrandService, "clientBrandService");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equalsIgnoreCase("minecraft:brand")) {
            return;
        }
        clientBrandService.setBrand(player.getUniqueId(), readMinecraftString(message));
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        clientBrandService.registerChannel(event.getPlayer().getUniqueId(), event.getChannel());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clientBrandService.remove(event.getPlayer().getUniqueId());
    }

    private String readMinecraftString(byte[] data) {
        if (data.length == 0) {
            return "unknown";
        }
        int[] offset = {0};
        int length = readVarInt(data, offset);
        if (length >= 0 && offset[0] + length <= data.length) {
            return new String(data, offset[0], length, StandardCharsets.UTF_8);
        }
        return new String(data, StandardCharsets.UTF_8).replace("\u0000", "").strip();
    }

    private int readVarInt(byte[] data, int[] offset) {
        int result = 0;
        int numRead = 0;
        byte read;
        do {
            if (offset[0] >= data.length || numRead > 4) {
                return -1;
            }
            read = data[offset[0]++];
            int value = read & 0b01111111;
            result |= value << (7 * numRead);
            numRead++;
        } while ((read & 0b10000000) != 0);
        return result;
    }
}
