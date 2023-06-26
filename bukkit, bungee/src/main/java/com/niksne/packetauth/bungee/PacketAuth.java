package com.niksne.packetauth.bungee;

import com.niksne.packetauth.bungee.auth.PlayerQueue;
import com.niksne.packetauth.bungee.auth.PlayerQueueBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.nio.ByteBuffer;

public final class PacketAuth extends Plugin implements Listener {
    public ConfigManager config;

    @Override
    public void onEnable() {
        this.config = new ConfigManager(this);
        this.getProxy().getPluginManager().registerListener(this, this);

        this.getProxy().registerChannel("packetauth:auth");

        new PlayerQueueBuilder(this).build();
    }

    @EventHandler
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (!event.getTag().equals("packetauth:auth")) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();
        ByteBuffer buffer = ByteBuffer.wrap(event.getData());
        StringBuilder builder = new StringBuilder();

        while (buffer.hasRemaining()) {
            builder.append((char) buffer.get());
        }

        config.reload();
        this.getLogger().info(player.getName() + "'s token is " + builder);

        if (builder.toString().equals(this.config.getString(player.getName()))) {
            PlayerQueue.getInstance().allow(player);
        }
    }
}