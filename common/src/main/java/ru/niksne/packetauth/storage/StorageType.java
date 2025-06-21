package ru.niksne.packetauth.storage;

import org.jetbrains.annotations.NotNull;

public enum StorageType {
    File("file"),
    MySQL("mysql"),
    MariaDB("mariadb"),
    PostgreSQL("postgresql");

    @NotNull
    private final String storageType;
    StorageType(
            @NotNull
            String storageType
    ) {
        this.storageType = storageType;
    }

    @NotNull
    public String toString(){
        return this.storageType;
    }
}
