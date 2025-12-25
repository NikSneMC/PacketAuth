package ru.niksne.packetauth.fabric.client;

import org.jetbrains.annotations.NotNull;
import ru.niksne.packetauth.storage.file.StorageManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import ru.niksne.packetauth.storage.file.TokenStorage;

public class PacketAuth implements ClientModInitializer {

    private static final StorageManager<TokenStorage> tokenStorageManager = new StorageManager<>(
        TokenStorage.class,
        FabricLoader.getInstance().getConfigDir().resolve("PacketAuth"),
        "tokens"
    );

    @Override
    public void onInitializeClient() {
        new MigrateConfig(tokenStorageManager);

        new Handler();
    }

    @NotNull
    public static StorageManager<TokenStorage> getTokenStorageManager() {
        return tokenStorageManager;
    }

    @NotNull
    public static TokenStorage getTokenStorage() {
        return tokenStorageManager.getStorage();
    }
}
