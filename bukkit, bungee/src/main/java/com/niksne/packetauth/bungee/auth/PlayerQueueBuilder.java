package com.niksne.packetauth.bungee.auth;


import com.niksne.packetauth.bungee.PacketAuth;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.function.Consumer;

public class PlayerQueueBuilder {

    private final PacketAuth plugin;
    private Consumer<ProxiedPlayer> onJoin;
    private Consumer<ProxiedPlayer> onAccepted;
    private Consumer<ProxiedPlayer> onLeave;

    public PlayerQueueBuilder(PacketAuth plugin) {
        this.plugin = plugin;
    }

    public PlayerQueueBuilder onJoin(Consumer<ProxiedPlayer> onJoin) {
        this.onJoin = onJoin;
        return this;
    }

    public PlayerQueueBuilder onAccepted(Consumer<ProxiedPlayer> onAccepted) {
        this.onAccepted = onAccepted;
        return this;
    }

    public PlayerQueueBuilder onLeave(Consumer<ProxiedPlayer> onLeave) {
        this.onLeave = onLeave;
        return this;
    }

    public PlayerQueue build() {
        return new PlayerQueue(this.plugin, this.onJoin, this.onAccepted, this.onLeave);
    }
}