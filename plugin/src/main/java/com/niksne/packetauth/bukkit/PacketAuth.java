package com.niksne.packetauth.bukkit;

import com.niksne.packetauth.ConfigManager;
import com.niksne.packetauth.MigrateConfig;
import com.niksne.packetauth.MySQLManager;
import com.niksne.packetauth.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class PacketAuth extends JavaPlugin implements Listener, PluginMessageListener {
    private static ConfigManager config;
    private static ConfigManager tokens;

    private final Set<String> verified = new HashSet<>();
    private final Set<String> outdated = new HashSet<>();

    @Override
    public void onEnable() {
        config =  new ConfigManager(getDataFolder().getPath(), "config", "config");
        new MigrateConfig(config, tokens);

        this.getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getMessenger().registerIncomingPluginChannel(this, "packetauth:auth", this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "packetauth:token");

        Utils.checkAutogen(config);

        if (Utils.checkStorageType(config, false) == null) tokens = new ConfigManager(getDataFolder().getPath(), "tokens", "empty");
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals("packetauth:auth")) return;
        Utils.verify(message, outdated, player.getName(), config, tokens, verified);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        outdated.add(player.getName());

        MySQLManager db = Utils.checkStorageType(config, true);
        boolean autogenEnabled = Utils.checkAutogen(config);

        PacketAuth plugin = this;
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        long delay = (long) Utils.eval(config.getString("kick.delay").replace("%ping%", String.valueOf(player.getPing())));
        service.scheduleWithFixedDelay(
                () -> {
                    if (player.isOnline()) {
                        if (outdated.contains(player.getName())) player.kickPlayer(config.getString("kick.outdated").replace("%version%", "1.6").replace("&", "§"));
                        else {
                            if (autogenEnabled && db == null && !tokens.containsKey(player.getName())) {
                                String token = Utils.generateRandomToken(config.getString("tokengen.symbols").replace(";", ""), Integer.parseInt(config.getString("tokengen.length")));
                                tokens.putString(player.getName(), token);
                                player.sendPluginMessage(plugin, "packetauth:token", token.getBytes());
                            } else if (autogenEnabled && db != null && db.noPlayer(player.getName())) {
                                String token = Utils.generateRandomToken(config.getString("tokengen.symbols").replace(";", ""), Integer.parseInt(config.getString("tokengen.length")));
                                db.saveToken(player.getName(), token);
                                player.sendPluginMessage(plugin, "packetauth:token", token.getBytes());
                            } else {
                                if (!verified.contains(player.getName())) player.kickPlayer(config.getString("kick.message").replace("%name%", player.getName()).replace("&", "§"));
                                else verified.remove(player.getName());
                            }
                        }
                        outdated.remove(player.getName());
                    }
                    service.shutdown();
                }, delay, delay, TimeUnit.MILLISECONDS
        );
    }
}