package com.niksne.packetauth.bukkit;

import com.niksne.packetauth.bukkit.auth.PlayerQueue;
import com.niksne.packetauth.bukkit.auth.PlayerQueueBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.ByteBuffer;

public final class PacketAuth extends JavaPlugin implements Listener, PluginMessageListener {

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "packetauth:auth");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "packetauth:auth", this);

        new PlayerQueueBuilder(this).build();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("packetauth:auth")) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.wrap(message);
        StringBuilder builder = new StringBuilder();

        while (buffer.hasRemaining()) {
            builder.append((char) buffer.get());
        }
        reloadConfig();
        this.getLogger().info(player.getName() + "'s token is " + builder);
        if (builder.toString().equals(this.getConfig().getString(player.getName()))) {
                PlayerQueue.getInstance().allow(player);
        }
    }
}