package com.niksne.packetauth.server;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.niksne.packetauth.server.Utils.*;

public final class PacketAuth implements DedicatedServerModInitializer, ServerPlayNetworking.PlayChannelHandler {

    private static final Identifier AUTH_PACKET_ID = new Identifier("packetauth:auth");

    private final Set<ServerPlayerEntity> verified = new HashSet<>();

    @Override
    public void onInitializeServer() {
        ServerPlayNetworking.registerGlobalReceiver(AUTH_PACKET_ID, this);
        generateConfig();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            long delay = (long) eval(getKickDelay().replace("%ping%", String.valueOf(player.pingMilliseconds)));
            ScheduledExecutorService service  = Executors.newScheduledThreadPool(1);
            service.scheduleWithFixedDelay(
                    () -> {
                        if (!player.isDisconnected() && !verified.contains(player)) {
                            player.networkHandler.disconnect(Text.of(getKickMsg().replace("%name%", player.getEntityName())));
                        } else verified.remove(player);
                        service.shutdown();
                        }, delay, delay, TimeUnit.MILLISECONDS
            );
        });

    }

    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        String token = getToken(player.getEntityName());
        if (token == null) { return; }
        if (new String(buf.getWrittenBytes(), StandardCharsets.UTF_8).equals(token)) { verified.add(player); }
        System.out.println(verified);
    }
}