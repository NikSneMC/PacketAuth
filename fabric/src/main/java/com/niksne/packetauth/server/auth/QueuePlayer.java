package com.niksne.packetauth.server.auth;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.ScheduledExecutorService;

public record QueuePlayer(ServerPlayerEntity player, ScheduledExecutorService service) {

}