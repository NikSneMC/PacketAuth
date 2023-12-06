package com.niksne.packetauth.client;

import com.niksne.packetauth.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.util.Objects;

public class MigrateConfig {
    private static final File serversDatFile = new File(FabricLoader.getInstance().getGameDir() + "/tokens.dat");
    public MigrateConfig(ConfigManager config) {
        try {
            if (serversDatFile.exists()) {
                NbtCompound nbt = NbtIo.read(serversDatFile.toPath());
                assert nbt != null;
                for (String key: nbt.getKeys()) {
                    config.addString(
                            key,
                            Objects.requireNonNull(
                                    Objects.requireNonNull(
                                            NbtIo.read(serversDatFile.toPath())
                                    ).get(key)
                            ).asString()
                    );
                serversDatFile.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}