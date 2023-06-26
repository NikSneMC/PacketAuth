package com.niksne.packetauth.server.auth;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.Consumer;

public class PlayerQueueBuilder {

    private Consumer<ServerPlayerEntity> onJoin;
    private Consumer<ServerPlayerEntity> onAccepted;
    private Consumer<ServerPlayerEntity> onLeave;


    public PlayerQueueBuilder() {
    }

    public PlayerQueueBuilder onJoin(Consumer<ServerPlayerEntity> onJoin) {
        this.onJoin = onJoin;
        return this;
    }

    public PlayerQueueBuilder onAccepted(Consumer<ServerPlayerEntity> onAccepted) {
        this.onAccepted = onAccepted;
        return this;
    }

    public PlayerQueueBuilder onLeave(Consumer<ServerPlayerEntity> onLeave) {
        this.onLeave = onLeave;
        return this;
    }

    public PlayerQueue build() {
            return new PlayerQueue(this.onJoin, this.onAccepted, this.onLeave);
    }
}