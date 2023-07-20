package com.niksne.packetauth.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.niksne.packetauth.Utils.eval;

public final class PacketAuth extends Plugin implements Listener {
    public ConfigManager config;
    private final Set<String> verified = new HashSet<>();

    @Override
    public void onEnable() {
        this.config = new ConfigManager(this);
        this.getProxy().getPluginManager().registerListener(this, this);
        this.getProxy().registerChannel("packetauth:auth");
    }

    @EventHandler
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (!event.getTag().equals("packetauth:auth")) { return; }
        ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();
        config.reload();
        String token = config.getString(player.getName());
        if (token == null) return;
        if (new String(event.getData(), StandardCharsets.UTF_8).equals(token)) verified.add(player.getName());
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        ProxiedPlayer player = getProxy().getPlayer(event.getConnection().getName());
        config.reload();
        getProxy().getScheduler().schedule(this, () -> {
            if (player.isConnected() && !verified.contains(player.getName())) player.disconnect(config.getString("kick.message"));
            else verified.remove(player.getName());
        }, (long) eval(config.getString("kick.delay").replace("%ping%", String.valueOf(player.getPing()))), TimeUnit.MILLISECONDS);
    }
}