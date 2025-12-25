package ru.niksne.packetauth.storage.file.sections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.niksne.packetauth.storage.StorageType;
import ru.niksne.packetauth.storage.database.DatabaseManager;

public class StorageSettings {
    @NotNull
    public StorageType mode = StorageType.File;
    @Nullable
    public String host = "localhost";
    @Nullable
    public Integer port = 3306;
    @Nullable
    public String database_name = "PacketAuth";
    @Nullable
    public String table_name = "Tokens";
    @Nullable
    public String user = "PacketAuth";
    @Nullable
    public String password = "PacketAuthPluginPassword1234";

    public void setStorageType(
        @NotNull
        String storageTypeStr
    ) {
        this.mode = switch (storageTypeStr) {
            case "file" -> StorageType.File;
            case "mysql" -> StorageType.MySQL;
            case "mariadb" -> StorageType.MariaDB;
            case "postgresql" -> StorageType.PostgreSQL;
            default -> throw new IllegalArgumentException("Invalid storage type: " + storageTypeStr);
        };
    }

    @Nullable
    public DatabaseManager checkStorageType() {
        if (this.mode == StorageType.File) {
            return null;
        }

        assert this.host != null
                && this.port != null
                && this.database_name != null
                && this.table_name != null
                && this.user != null
                && this.password != null;

        return new DatabaseManager(
                this.mode,
                this.host,
                this.port,
                this.database_name,
                this.table_name,
                this.user,
                this.password
        );
    }
}
