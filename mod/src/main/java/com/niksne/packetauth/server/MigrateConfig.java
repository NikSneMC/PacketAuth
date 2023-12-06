package com.niksne.packetauth.server;

import com.niksne.packetauth.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.util.Objects;

public class MigrateConfig {
    private static final File configDatFile = new File(FabricLoader.getInstance().getGameDir() + "/settings.dat");
    public MigrateConfig(ConfigManager config, ConfigManager tokens) {
        try {
            if (configDatFile.exists()) {
                config.putString(
                        "kick.message",
                        Objects.requireNonNull(
                                Objects.requireNonNull(
                                        NbtIo.read(configDatFile.toPath())
                                ).get("kick-message")
                        ).asString()
                );
                config.putString(
                        "kick.delay",
                        Objects.requireNonNull(
                            Objects.requireNonNull(
                                    NbtIo.read(configDatFile.toPath())
                            ).get("kick-delay")
                    ).asString()
                );
                for (String key: Objects.requireNonNull(
                        (NbtCompound) Objects.requireNonNull(
                                NbtIo.read(configDatFile.toPath())
                        ).get("tokens")
                ).getKeys()) {
                    tokens.addString(
                            key,
                            Objects.requireNonNull(
                                    Objects.requireNonNull(
                                            (NbtCompound) Objects.requireNonNull(
                                                    NbtIo.read(configDatFile.toPath())
                                            ).get("tokens")
                                    ).get(key)
                            ).asString()
                    );
                }
                configDatFile.delete();
            } else if (config.getString("config.version").compareTo("1.6.2") < 0) {
                config.putString("config.version", "1.6.2");
                config.addString("tokenDisabling.enabled", "false");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}