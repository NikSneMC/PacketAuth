package ru.niksne.packetauth.server;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import ru.niksne.packetauth.*;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import ru.niksne.packetauth.payload.AuthPayload;
import ru.niksne.packetauth.payload.TokenPayload;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class PacketAuth implements DedicatedServerModInitializer, ServerPlayNetworking.PlayPayloadHandler<AuthPayload> {

    private static final ConfigManager config = new ConfigManager(FabricLoader.getInstance().getGameDir() + "/config/PacketAuth","config", "config");
    private static ConfigManager tokens;
    private static ConfigManager disabled;

    private static MySQLManager db;

    private final Set<String> verified = new HashSet<>();
    private Set<String> outdated = new HashSet<>();

    @Override
    public void onInitializeServer() {
        PayloadTypeRegistry.playC2S().register(AuthPayload.ID, AuthPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TokenPayload.ID, TokenPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AuthPayload.ID, this);

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
                            case "send_token" -> ServerPlayNetworking.send(player, new TokenPayload(checker.getToken()));
                            case "pass" -> verified.remove(player.getName().getString());
                        }
                    }
                    outdated.remove(player.getName().getString());
                }, preparer.getDelay(), preparer.getDelay(), TimeUnit.MILLISECONDS
            );
        });

    }

    @Override
    public void receive(AuthPayload payload, ServerPlayNetworking.Context context) {
        Utils.verify(payload.token().getBytes(), outdated, context.player().getName().getString(), config, tokens, verified);
    }
}