package com.niksne.packetauth.server;

import com.niksne.packetauth.server.auth.PlayerQueue;
import com.niksne.packetauth.server.auth.PlayerQueueBuilder;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.nio.ByteBuffer;

import static com.niksne.packetauth.server.Utils.generateConfig;
import static com.niksne.packetauth.server.Utils.getToken;

public final class PacketAuth implements DedicatedServerModInitializer, ServerPlayNetworking.PlayChannelHandler {

    private static final Identifier AUTH_PACKET_ID = new Identifier("packetauth:auth");

    @Override
    public void onInitializeServer() {
        ServerPlayNetworking.registerGlobalReceiver(AUTH_PACKET_ID, this);
        new PlayerQueueBuilder().build();
        generateConfig();
    }

    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        ByteBuffer buffer = ByteBuffer.wrap(buf.getWrittenBytes());
        StringBuilder builder = new StringBuilder();
        while (buffer.hasRemaining()) {
            builder.append((char) buffer.get());
        }

        String name = player.getEntityName();
        System.out.println(name + "'s token is " + builder);

        if (builder.toString().equals(getToken(name))) {
            PlayerQueue.getInstance().allow(player);
        }
    }

}