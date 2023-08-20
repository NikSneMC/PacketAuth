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

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
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

        if (Boolean.parseBoolean(config.getString("tokengen.enabled"))) {
            config.addString("tokengen.length", "4096");
            config.addString("tokengen.symbols", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        } else {
            config.removeString("tokengen.length");
            config.removeString("tokengen.symbols");
        }

        if (config.getString("storage.mode").equals("mysql")) {
            config.addString("storage.mysql.host", "localhost");
            config.addString("storage.mysql.port", "3306");
            config.addString("storage.mysql.databaseName", "PacketAuth");
            config.addString("storage.mysql.tableName", "Tokens");
            config.addString("storage.mysql.user", "PacketAuth");
            config.addString("storage.mysql.password", "PacketAuthPluginPassword1234");
        } else {
            tokens = new ConfigManager(FabricLoader.getInstance().getGameDir() + "/config/PacketAuth","tokens", "empty");
            config.removeString("storage.mysql.host");
            config.removeString("storage.mysql.port");
            config.removeString("storage.mysql.databaseName");
            config.removeString("storage.mysql.tableName");
            config.removeString("storage.mysql.user");
            config.removeString("storage.mysql.password");
        }

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            outdated.add(player.getEntityName());

            boolean autogenEnabled = Boolean.parseBoolean(config.getString("tokengen.enabled"));
            if (autogenEnabled) {
                config.addString("tokengen.length", "4096");
                config.addString("tokengen.symbols", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
            } else {
                config.removeString("tokengen.length");
                config.removeString("tokengen.symbols");
            }

            boolean MySQLEnabled = config.getString("storage.mode").equals("mysql");
            MySQLManager db;
            if (MySQLEnabled) {
                config.addString("storage.mysql.host", "localhost");
                config.addString("storage.mysql.port", "3306");
                config.addString("storage.mysql.databaseName", "PacketAuth");
                config.addString("storage.mysql.tableName", "Tokens");
                config.addString("storage.mysql.user", "PacketAuth");
                config.addString("storage.mysql.password", "PacketAuthPluginPassword1234");
                db = new MySQLManager(
                        config.getString("storage.mysql.host"),
                        Integer.parseInt(config.getString("storage.mysql.port")),
                        config.getString("storage.mysql.databaseName"),
                        config.getString("storage.mysql.tableName"),
                        config.getString("storage.mysql.user"),
                        config.getString("storage.mysql.password")
                );
            } else {
                db = null;
                config.removeString("storage.mysql.host");
                config.removeString("storage.mysql.port");
                config.removeString("storage.mysql.databaseName");
                config.removeString("storage.mysql.tableName");
                config.removeString("storage.mysql.user");
                config.removeString("storage.mysql.password");
            }

            ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
            long delay = (long) Utils.eval(config.getString("kick.delay").replace("%ping%", String.valueOf(player.pingMilliseconds)));

            service.scheduleWithFixedDelay(
                    () -> {
                        if (!player.isDisconnected()) {
                            if (outdated.contains(player.getEntityName())) player.networkHandler.disconnect(Text.of(config.getString("kick.outdated").replace("%version%", "1.6").replace("&", "§")));
                            else {
                                if (autogenEnabled && !MySQLEnabled && !tokens.containsKey(player.getEntityName())) {
                                    String token = Utils.generateRandomToken(config.getString("tokengen.symbols").replace(";", ""), Integer.parseInt(config.getString("tokengen.length")));
                                    tokens.putString(player.getEntityName(), token);
                                    ServerPlayNetworking.send(player, AUTH_TOKEN, new PacketByteBuf(Unpooled.wrappedBuffer(token.getBytes())));
                                } else if (autogenEnabled && MySQLEnabled && !db.hasPlayer(player.getEntityName())) {
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
        String income = new String(buf.getWrittenBytes(), StandardCharsets.UTF_8);
        if (!income.contains(";")) income = "0;" + income;
        List<String> msg = List.of(income.split(";"));
        if (msg.get(0).compareTo("1.6") >= 0) outdated.remove(player.getEntityName());
        if (config.getString("storage.mode").equals("mysql")) {
            MySQLManager db = new MySQLManager(
                    config.getString("storage.mysql.host"),
                    Integer.parseInt(config.getString("storage.mysql.port")),
                    config.getString("storage.mysql.databaseName"),
                    config.getString("storage.mysql.tableName"),
                    config.getString("storage.mysql.user"),
                    config.getString("storage.mysql.password")
            );
            if (!db.hasPlayer(player.getEntityName())) return;
            if (msg.get(1).equals(db.getToken(player.getEntityName()).replace(";", ""))) verified.add(player.getEntityName());
        } else {
            if (!tokens.containsKey(player.getEntityName())) return;
            if (msg.get(1).equals(tokens.getString(player.getEntityName()).replace(";", ""))) verified.add(player.getEntityName());
        }
    }
}