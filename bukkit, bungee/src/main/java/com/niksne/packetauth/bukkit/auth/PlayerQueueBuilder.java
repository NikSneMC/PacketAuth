package com.niksne.packetauth.bukkit.auth;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public class PlayerQueueBuilder {

    private final Plugin plugin;
    private Consumer<Player> onJoin;
    private Consumer<Player> onAccepted;
    private Consumer<Player> onLeave;

    public PlayerQueueBuilder(Plugin plugin) {
        this.plugin = plugin;
    }

    public PlayerQueueBuilder onJoin(Consumer<Player> onJoin) {
        this.onJoin = onJoin;
        return this;
    }

    public PlayerQueueBuilder onAccepted(Consumer<Player> onAccepted) {
        this.onAccepted = onAccepted;
        return this;
    }

    public PlayerQueueBuilder onLeave(Consumer<Player> onLeave) {
        this.onLeave = onLeave;
        return this;
    }

    public PlayerQueue build() {
        return new PlayerQueue(this.plugin, this.onJoin, this.onAccepted, this.onLeave);
    }
}