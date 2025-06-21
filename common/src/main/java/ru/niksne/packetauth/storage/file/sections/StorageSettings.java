package ru.niksne.packetauth.storage.file.sections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.niksne.packetauth.storage.StorageType;

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
}
