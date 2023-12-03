package com.niksne.packetauth.bukkit;

import com.niksne.packetauth.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class PacketAuth extends JavaPlugin implements Listener, PluginMessageListener {
    private static ConfigManager config;
    private static ConfigManager tokens;
    private static ConfigManager disabled;

    private static MySQLManager db;

    private static boolean isFolia;

    private final Set<String> verified = new HashSet<>();
    private Set<String> outdated = new HashSet<>();

    @Override
    public void onEnable() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
        config =  new ConfigManager(getDataFolder().getPath(), "config", "config");
        new MigrateConfig(config, tokens);

        this.getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getMessenger().registerIncomingPluginChannel(this, "packetauth:auth", this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "packetauth:token");

        Utils.checkAutogen(config);
        db = Utils.checkStorageType(config);

        if (db == null) tokens = new ConfigManager(getDataFolder().getPath(), "tokens", "empty");
        if (Utils.checkTokenDisabling(config, db)) disabled = new ConfigManager(getDataFolder().getPath(),"disabled_tokens", "empty");
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals("packetauth:auth")) return;
        Utils.verify(message, outdated, player.getName(), config, tokens, verified);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        LoginPreparer preparer = new LoginPreparer(config, db, outdated, player.getName(), player.getPing());
        outdated = preparer.getOutdated();
        db = preparer.getDb();
        if (isFolia) {
            preparer.getService().scheduleWithFixedDelay(
                    () -> {
                        preparer.getService().shutdown();
                        check(player, preparer);
                    }, preparer.getDelay(), preparer.getDelay(), TimeUnit.MILLISECONDS
            );
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                check(player, preparer);
            }
        }.runTaskLater(this, preparer.getDelay() / 20);
    }

    private void check(Player player, LoginPreparer preparer) {
        if (player.isOnline()) {
            LoginChecker checker = new LoginChecker(preparer, player.getName(), config, db, disabled, tokens, verified);
            switch (checker.getAction()) {
                case "kick" -> player.kickPlayer(ChatColor.translateAlternateColorCodes('&', checker.getReason()));
                case "send_token" -> player.sendPluginMessage(this, "packetauth:token", checker.getToken().getBytes());
                case "pass" -> verified.remove(player.getName());
            }
        }
        outdated.remove(player.getName());
    }
}
