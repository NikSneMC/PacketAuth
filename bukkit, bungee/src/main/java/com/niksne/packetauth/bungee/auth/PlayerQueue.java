package com.niksne.packetauth.bungee.auth;


import com.niksne.packetauth.bungee.ConfigManager;
import com.niksne.packetauth.bungee.PacketAuth;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.niksne.packetauth.Utils.eval;

public class PlayerQueue implements Listener {

    private static PlayerQueue instance;

    private final Map<String, QueuePlayer> queue = new HashMap<>();

    private final PacketAuth plugin;
    private final Consumer<ProxiedPlayer> onJoin;
    private final Consumer<ProxiedPlayer> onAccepted;
    private final Consumer<ProxiedPlayer> onLeave;

    public PlayerQueue(PacketAuth plugin, Consumer<ProxiedPlayer> onJoin, Consumer<ProxiedPlayer> onAccepted, Consumer<ProxiedPlayer> onLeave) {
        this.plugin = plugin;

        this.onJoin = onJoin;
        this.onAccepted = onAccepted;
        this.onLeave = onLeave;

        instance = this;
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(event.getConnection().getName());

        if (this.onJoin != null) {
            this.onJoin.accept(player);
        }
        this.plugin.config.reload();
        ConfigManager cfg = this.plugin.config;
        this.queue.put(player.getName(), new QueuePlayer(player,
            plugin.getProxy().getScheduler().schedule(this.plugin,
                () -> player.disconnect(
                        Objects.requireNonNull(
                                cfg.getString("kick.message")
                        ).replace("%name%", player.getName())
                ),
                (long) eval(Objects.requireNonNull(cfg.getString("kick.delay")).replace(
                        "%ping%",
                        String.valueOf(player.getPing())
                )),
                TimeUnit.MILLISECONDS
            )
        ));
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (this.onLeave != null) {
            this.onLeave.accept(player);
        }

        this.queue.remove(player.getName());
    }

    public void allow(ProxiedPlayer player) {
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