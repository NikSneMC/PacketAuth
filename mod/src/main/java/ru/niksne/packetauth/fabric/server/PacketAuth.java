package ru.niksne.packetauth.fabric.server;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import ru.niksne.packetauth.storage.ManagerWrapper;
import ru.niksne.packetauth.storage.MigrateStorage;

import java.nio.file.Path;

public final class PacketAuth implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        Path dataDir = FabricLoader.getInstance().getConfigDir().resolve("PacketAuth");

        ManagerWrapper.init(dataDir);

        new MigrateStorage();
        new Handler();
    }
}