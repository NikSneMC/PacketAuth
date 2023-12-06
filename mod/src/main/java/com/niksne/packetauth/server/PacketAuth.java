package com.niksne.packetauth.server;

import com.niksne.packetauth.*;
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
import java.util.concurrent.TimeUnit;

public final class PacketAuth implements DedicatedServerModInitializer, ServerPlayNetworking.PlayChannelHandler {

    private static final Identifier AUTH_PACKET_ID = new Identifier("packetauth:auth");
    private static final Identifier AUTH_TOKEN = new Identifier("packetauth:token");

    private static final ConfigManager config = new ConfigManager(FabricLoader.getInstance().getGameDir() + "/config/PacketAuth","config", "config");
    private static ConfigManager tokens;
    private static ConfigManager disabled;

    private static MySQLManager db;

    private final Set<String> verified = new HashSet<>();
    private Set<String> outdated = new HashSet<>();

    @Override
    public void onInitializeServer() {
        ServerPlayNetworking.registerGlobalReceiver(AUTH_PACKET_ID, this);

        new MigrateConfig(config, tokens);
        Utils.checkAutogen(config);
        db = Utils.checkStorageType(config);

        if (db == null) tokens = new ConfigManager(FabricLoader.getInstance().getGameDir() + "/config/PacketAuth","tokens", "empty");
        if (Utils.checkTokenDisabling(config, db)) disabled = new ConfigManager(FabricLoader.getInstance().getGameDir() + "/config/PacketAuth","disabled_tokens", "empty");

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            LoginPreparer preparer = new LoginPreparer(config, db, outdated, player.getName().getString(), handler.getLatency());
            outdated = preparer.getOutdated();
            db = preparer.getDb();
            preparer.getService().scheduleWithFixedDelay(
                () -> {
                    preparer.getService().shutdown();
                    if (!player.isDisconnected()) {
                        LoginChecker checker = new LoginChecker(preparer, player.getName().getString(), config, db, disabled, tokens, verified);
                        switch (checker.getAction()) {
                            case "kick" -> player.networkHandler.disconnect(Text.of(checker.getReason().replace("&", "§")));
                            case "send_token" -> ServerPlayNetworking.send(player, AUTH_TOKEN, new PacketByteBuf(Unpooled.wrappedBuffer(checker.getToken().getBytes())));
                            case "pass" -> verified.remove(player.getName().getString());
                        }
                    }
                    outdated.remove(player.getName().getString());
                }, preparer.getDelay(), preparer.getDelay(), TimeUnit.MILLISECONDS
            );
        });

    }

    @Override
    public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        Utils.verify(buf.copy().array(), outdated, player.getName().getString(), config, tokens, verified);
    }
}