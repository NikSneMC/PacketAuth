package ru.niksne.packetauth.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.niksne.packetauth.Utils;
import ru.niksne.packetauth.storage.database.DatabaseManager;
import ru.niksne.packetauth.storage.file.ConfigStorage;
import ru.niksne.packetauth.storage.file.DisabledTokenStorage;
import ru.niksne.packetauth.storage.file.StorageManager;
import ru.niksne.packetauth.storage.file.TokenStorage;

import java.nio.file.Path;

public class ManagerWrapper {
    private static StorageManager<ConfigStorage> configStorageManager;
    @Nullable
    private static StorageManager<TokenStorage> tokenStorageManager;
    @Nullable
    private static StorageManager<DisabledTokenStorage> disabledTokenStorageManager;

    @Nullable
    private static DatabaseManager databaseManager;

    public static void init(
            @NotNull
            Path dataFolder
    ) {
        configStorageManager = new StorageManager<>(ConfigStorage.class, dataFolder, "config");

        databaseManager = Utils.checkStorageType(configStorageManager.getStorage().storage);

        if (databaseManager == null) {
            tokenStorageManager = new StorageManager<>(TokenStorage.class, dataFolder, "tokens");
        }
        if (Utils.checkTokenDisabling(configStorageManager.getStorage(), databaseManager)) {
            disabledTokenStorageManager = new StorageManager<>(DisabledTokenStorage.class, dataFolder, "disabled_tokens");
        }
    }

    @NotNull
    public static StorageManager<ConfigStorage> getConfigStorageManager() {
        return configStorageManager;
    }

    @NotNull
    public static ConfigStorage getConfigStorage() {
        return configStorageManager.getStorage();
    }

    @Nullable
    public static StorageManager<TokenStorage> getTokenStorageManager() {
        return tokenStorageManager;
    }

    @Nullable
    public static TokenStorage getTokenStorage() {
        if (tokenStorageManager == null) {
            return null;
        }
        return tokenStorageManager.getStorage();
    }

    @Nullable
    public static StorageManager<DisabledTokenStorage> getDisabledTokenStorageManager() {
        return disabledTokenStorageManager;
    }

    @Nullable
    public static DisabledTokenStorage getDisabledTokenStorage() {
        if (disabledTokenStorageManager == null) {
            return null;
        }
        return disabledTokenStorageManager.getStorage();
    }

    @Nullable
    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public static void setDatabaseManager(
            @Nullable
            DatabaseManager databaseManager
    ) {
        ManagerWrapper.databaseManager = databaseManager;
    }
}
