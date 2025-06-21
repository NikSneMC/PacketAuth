package ru.niksne.packetauth.login;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.niksne.packetauth.Utils;
import ru.niksne.packetauth.storage.ManagerWrapper;
import ru.niksne.packetauth.storage.database.DatabaseManager;
import ru.niksne.packetauth.storage.file.ConfigStorage;
import ru.niksne.packetauth.storage.file.DisabledTokenStorage;
import ru.niksne.packetauth.storage.file.StorageManager;
import ru.niksne.packetauth.storage.file.TokenStorage;

import java.util.Set;
import java.util.concurrent.Executors;

public class LoginFlow {
    @NotNull
    public static LoginPreparation prepare(
            @NotNull
            Set<String> outdated,
            @NotNull
            String name,
            @NotNull
            Long ping
    ) {
        ConfigStorage configStorage = ManagerWrapper.getConfigStorage();

        outdated.add(name);
        DatabaseManager databaseManager = Utils.checkStorageType(configStorage.storage);
        ManagerWrapper.setDatabaseManager(databaseManager);
        long p = ping;
        if (p <= 0) {
            p = 1;
        }
        long delay = (long) Utils.eval(configStorage.kick.delay.replaceAll("%ping%", String.valueOf(p)));

        return new LoginPreparation(
                Executors.newScheduledThreadPool(1),
                delay
        );
    }

    @NotNull
    public static LoginCheckerAction check(
            @NotNull
            Set<String> outdated,
            @NotNull
            String name,
            @NotNull
            Set<String> verified
    ) {
        @NotNull
        ConfigStorage configStorage = ManagerWrapper.getConfigStorage();
        @Nullable
        StorageManager<TokenStorage> tokenStorageManager = ManagerWrapper.getTokenStorageManager();
        @Nullable
        DatabaseManager databaseManager = ManagerWrapper.getDatabaseManager();
        @Nullable
        DisabledTokenStorage disabledTokenStorage = ManagerWrapper.getDisabledTokenStorage();

        if (outdated.contains(name)) {
            String reason = configStorage.kick.outdated
                    .replaceAll("%name%", name)
                    .replaceAll("%version%", "1.6");
            return new LoginCheckerAction.Kick(reason);
        }

        boolean tokenGenEnabled = configStorage.tokengen.enabled;
        if (tokenGenEnabled && tokenStorageManager != null && !tokenStorageManager.getStorage().hasTokenFor(name)) {
            String token = Utils.generateRandomToken(configStorage.tokengen);
            tokenStorageManager.getStorage().saveTokenFor(name, token);
            tokenStorageManager.saveStorage();
            return new LoginCheckerAction.SendToken(token);
        }

        if (tokenGenEnabled && databaseManager != null && !databaseManager.hasRecord(configStorage.storage.table_name, "name", name)) {
            String token = Utils.generateRandomToken(configStorage.tokengen);
            databaseManager.saveToken(name, token);
            return new LoginCheckerAction.SendToken(token);
        }

        if (!verified.contains(name)) {
            String reason = configStorage.kick.message
                    .replaceAll("%name%", name);
            return new LoginCheckerAction.Kick(reason);
        }

        boolean tokenDisablingEnabled = Utils.checkTokenDisabling(configStorage, databaseManager);
        if (tokenDisablingEnabled && disabledTokenStorage != null && disabledTokenStorage.isTokenDisabledFor(name)) {
            String reason = Utils.parseMessage(
                    configStorage.kick,
                    name,
                    disabledTokenStorage.getTokenDisablingReason(name)
            );
            return new LoginCheckerAction.Kick(reason);
        }

        String tableName = configStorage.token_disabling.table_name;
        if (tokenDisablingEnabled && databaseManager != null && databaseManager.hasRecord(tableName, "name", name)) {
            String reason = Utils.parseMessage(
                    configStorage.kick,
                    name,
                    databaseManager.getReason(tableName, name)
            );
            return new LoginCheckerAction.Kick(reason);
        }

        return new LoginCheckerAction.Pass();
    }
}
