package ru.niksne.packetauth.login;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.niksne.packetauth.utils.MathParser;
import ru.niksne.packetauth.storage.ManagerWrapper;
import ru.niksne.packetauth.storage.database.DatabaseManager;
import ru.niksne.packetauth.storage.file.ConfigStorage;
import ru.niksne.packetauth.storage.file.DisabledTokenStorage;
import ru.niksne.packetauth.storage.file.StorageManager;
import ru.niksne.packetauth.storage.file.TokenStorage;
import ru.niksne.packetauth.storage.file.sections.StorageSettings;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;

public class LoginFlow {
    @NotNull
    public static LoginPreparation prepare(
        @NotNull
        Set<String> outdated,
        @NotNull
        String name,
        long ping
    ) {
        ConfigStorage configStorage = ManagerWrapper.getConfigStorage();

        outdated.add(name);
        DatabaseManager databaseManager = configStorage.storage.checkStorageType();
        ManagerWrapper.setDatabaseManager(databaseManager);
        long p = ping;
        if (p <= 0) {
            p = 1;
        }
        String expression = configStorage.kick.delay.replaceAll("%ping%", String.valueOf(p));
        long delay = new MathParser(expression).parseLong();

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
        if (
            tokenGenEnabled
            && tokenStorageManager != null
            && databaseManager == null
            && !tokenStorageManager.getStorage().hasTokenFor(name)
        ) {
            String token = configStorage.tokengen.generateRandomToken();
            tokenStorageManager.getStorage().saveTokenFor(name, token);
            tokenStorageManager.saveStorage();
            return new LoginCheckerAction.SendToken(token);
        }

        String storageTableName = configStorage.storage.table_name;
        if (
            tokenGenEnabled
            && databaseManager != null
            && storageTableName != null
            && !databaseManager.hasRecord(storageTableName, "name", name)
        ) {
            String token = configStorage.tokengen.generateRandomToken();
            databaseManager.saveToken(name, token);
            return new LoginCheckerAction.SendToken(token);
        }

        if (!verified.contains(name)) {
            String reason = configStorage.kick.message
                .replaceAll("%name%", name);
            return new LoginCheckerAction.Kick(reason);
        }

        boolean tokenDisablingEnabled = configStorage.checkTokenDisabling(databaseManager);
        if (
            tokenDisablingEnabled
            && disabledTokenStorage != null
            && disabledTokenStorage.isTokenDisabledFor(name)
        ) {
            String reason = configStorage.kick.parseMessage(
                name,
                disabledTokenStorage.getTokenDisablingReason(name)
            );
            return new LoginCheckerAction.Kick(reason);
        }

        String tokenDisablingTableName = configStorage.token_disabling.table_name;
        if (
            tokenDisablingEnabled
            && databaseManager != null
            && tokenDisablingTableName != null
            && databaseManager.hasRecord(tokenDisablingTableName, "name", name)
        ) {
            String reason = configStorage.kick.parseMessage(
                name,
                databaseManager.getReason(tokenDisablingTableName, name)
            );
            return new LoginCheckerAction.Kick(reason);
        }

        return new LoginCheckerAction.Pass();
    }

    public static void verify(
            byte[] input,
            @NotNull
            Set<String> outdated,
            @NotNull
            String name,
            @NotNull
            Set<String> verified
    ) {
        @NotNull
        StorageSettings storage = ManagerWrapper.getConfigStorage().storage;
        @Nullable
        TokenStorage tokenStorage = ManagerWrapper.getTokenStorage();

        String income = new String(input, StandardCharsets.UTF_8);
        if (!income.contains(";")) {
            income = "0;" + income;
        }

        List<String> msg = new java.util.ArrayList<>(List.of(income.split(";")));
        if (msg.size() == 1) {
            msg.add("");
        }
        if (msg.get(0).compareTo("1.6") >= 0) {
            outdated.remove(name);
        }

        DatabaseManager db = storage.checkStorageType();
        if (tokenStorage != null) {
            if (!tokenStorage.hasTokenFor(name)) {
                return;
            }

            if (msg.get(1).equals(tokenStorage.getTokenFor(name).replace(";", ""))) {
                verified.add(name);
            }
        } else if (db != null) {
            assert storage.table_name != null;
            if (!db.hasRecord(storage.table_name, "name", name)) {
                return;
            }

            if (msg.get(1).equals(Objects.requireNonNull(db.getToken(name)).replace(";", ""))) {
                verified.add(name);
            }
        }
    }
}
