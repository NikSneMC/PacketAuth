package com.niksne.packetauth.server;

import com.niksne.packetauth.ConfigManager;
import com.niksne.packetauth.MySQLManager;
import com.niksne.packetauth.Utils;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class PacketAuth implements DedicatedServerModInitializer, ServerPlayNetworking.PlayChannelHandler {

    private static final Identifier AUTH_PACKET_ID = new Identifier("packetauth:auth");
    private static final Identifier AUTH_TOKEN = new Identifier("packetauth:token");

    private static final ConfigManager config = new ConfigManager(FabricLoader.getInstance().getGameDir() + "/config/PacketAuth","config", "config");
    private static ConfigManager tokens;

    private final Set<String> verified = new HashSet<>();
    private final Set<String> outdated = new HashSet<>();

    @Override
    public void onInitializeServer() {
        ServerPlayNetworking.registerGlobalReceiver(AUTH_PACKET_ID, this);

        new MigrateConfig(config, tokens);

        if (Utils.checkStorageType(config, false) == null) tokens = new ConfigManager(FabricLoader.getInstance().getGameDir() + "/config/PacketAuth","tokens", "empty");

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            outdated.add(player.getEntityName());

            MySQLManager db = Utils.checkStorageType(config, true);
            boolean autogenEnabled = Utils.checkAutogen(config);

            ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
            long delay = (long) Utils.eval(config.getString("kick.delay").replace("%ping%", String.valueOf(player.pingMilliseconds)));

            service.scheduleWithFixedDelay(
                    () -> {
                        if (!player.isDisconnected()) {
                            if (outdated.contains(player.getEntityName())) player.networkHandler.disconnect(Text.of(config.getString("kick.outdated").replace("%version%", "1.6").replace("&", "§")));
                            else {
                                if (autogenEnabled && db == null && !tokens.containsKey(player.getEntityName())) {
                                    String token = Utils.generateRandomToken(config.getString("tokengen.symbols").replace(";", ""), Integer.parseInt(config.getString("tokengen.length")));
                                    tokens.putString(player.getEntityName(), token);
                                    ServerPlayNetworking.send(player, AUTH_TOKEN, new PacketByteBuf(Unpooled.wrappedBuffer(token.getBytes())));
                                } else if (autogenEnabled && db != null && db.noPlayer(player.getEntityName())) {
                                    String token = Utils.generateRandomToken(config.getString("tokengen.symbols").replace(";", ""), Integer.parseInt(config.getString("tokengen.length")));
                                    db.saveToken(player.getEntityName(), token);
                                    ServerPlayNetworking.send(player, AUTH_TOKEN, new PacketByteBuf(Unpooled.wrappedBuffer(token.getBytes())));
                                } else {
                                    if (!verified.contains(player.getEntityName())) player.networkHandler.disconnect(Text.of(config.getString("kick.message").replace("%name%", player.getEntityName()).replace("&", "§")));
                                    else verified.remove(player.getEntityName());
                                }
                            }
                            outdated.remove(player.getEntityName());
                        }
                        service.shutdown();
                    }, delay, delay, TimeUnit.MILLISECONDS
            );
        });

    }

    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        Utils.verify(buf.getWrittenBytes(), outdated, player.getEntityName(), config, tokens, verified);
    }
}