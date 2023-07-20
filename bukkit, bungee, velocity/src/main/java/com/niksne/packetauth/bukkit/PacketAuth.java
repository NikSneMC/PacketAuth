package com.niksne.packetauth.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static com.niksne.packetauth.Utils.eval;

public final class PacketAuth extends JavaPlugin implements Listener, PluginMessageListener {
    private final Set<String> verified = new HashSet<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "packetauth:auth", this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("packetauth:auth")) return;
        reloadConfig();
        String token = getConfig().getString(player.getName());
        if (token == null) return;
        if (new String(message, StandardCharsets.UTF_8).equals(token)) verified.add(player.getName());
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        reloadConfig();
        FileConfiguration cfg = getConfig();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && !verified.contains(player.getName())) player.kickPlayer(cfg.getString("kick.message"));
                else verified.remove(player.getName());
            }
        }.runTaskLater(this, (long) eval(cfg.getString("kick.delay").replace("%ping%", String.valueOf(player.getPing()))));
    }
}