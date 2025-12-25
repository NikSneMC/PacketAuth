package ru.niksne.packetauth.storage.file;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.niksne.packetauth.storage.StorageType;
import ru.niksne.packetauth.storage.database.DatabaseManager;
import ru.niksne.packetauth.storage.file.sections.*;

public class ConfigStorage {
    @NotNull
    static final String base = "config";

    @NotNull
    public ConfigInfo config = new ConfigInfo();
    @NotNull
    public KickSettings kick = new KickSettings();
    @NotNull
    public StorageSettings storage = new StorageSettings();
    @NotNull
    public TokenDisablingSettings token_disabling = new TokenDisablingSettings();
    @NotNull
    public TokenGenSettings tokengen = new TokenGenSettings();


    public boolean checkTokenDisabling(
        @Nullable
        DatabaseManager databaseManager
    ) {
        boolean tokenDisablingEnabled = this.token_disabling.enabled;

        if (tokenDisablingEnabled && this.storage.mode.equals(StorageType.MySQL) && databaseManager != null) {
            assert this.token_disabling.table_name != null;
            databaseManager.createTable(this.token_disabling.table_name, "reason");
        }

        return tokenDisablingEnabled;
    }
}
