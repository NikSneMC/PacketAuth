package com.niksne.packetauth.bukkit.auth;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static com.niksne.packetauth.Utils.eval;

public class PlayerQueue implements Listener {

    private static PlayerQueue instance;

    private final Map<String, QueuePlayer> queue = new HashMap<>();

    private final Plugin plugin;
    private final Consumer<Player> onJoin;
    private final Consumer<Player> onAccepted;
    private final Consumer<Player> onLeave;

    public PlayerQueue(Plugin plugin, Consumer<Player> onJoin, Consumer<Player> onAccepted, Consumer<Player> onLeave) {
        this.plugin = plugin;

        this.onJoin = onJoin;
        this.onAccepted = onAccepted;
        this.onLeave = onLeave;

        instance = this;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        if (this.onJoin != null) {
            this.onJoin.accept(player);
        }
        this.plugin.reloadConfig();
        FileConfiguration cfg = this.plugin.getConfig();
        this.queue.put(player.getName(), new QueuePlayer(player,
                Bukkit.getScheduler().runTaskLater(this.plugin,
                        () -> player.kickPlayer(
                                Objects.requireNonNull(
                                        cfg.getString("kick.message")
                                ).replace("%name%", player.getName())
                        ),
                        (long) eval(Objects.requireNonNull(cfg.getString("kick.delay")).replace(
                                "%ping%",
                                String.valueOf(player.getPing())
                                ))
                )
        ));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (this.onLeave != null) {
            this.onLeave.accept(player);
        }

        this.queue.remove(player.getName());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (this.queue.containsKey(player.getName()))
            event.setCancelled(true);
    }

    public void allow(Player player) {
        this.allow(player.getName());
    }

    public void allow(String name) {
        QueuePlayer player = this.queue.remove(name);
        player.task().cancel();

        if (this.onAccepted != null) {
            this.onAccepted.accept(player.player());
        }
    }

    public static PlayerQueue getInstance() {
        return instance;
    }
}