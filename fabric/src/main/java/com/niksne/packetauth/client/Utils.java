package com.niksne.packetauth.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.util.Objects;

public class Utils {
    private static final File serversDatFile = new File(FabricLoader.getInstance().getGameDir() + "/tokens.dat");
    public static void generateConfig() {
        try {
            if (!serversDatFile.exists()) {
                boolean success = serversDatFile.createNewFile();
                if (!success) { return; }
                NbtIo.write(new NbtCompound(), serversDatFile);
            }
        } catch (Exception e) {
            //
        }

    }
    public static void saveToken(String serverIp, String token) {
        try {
            generateConfig();
            NbtCompound nbt = NbtIo.read(serversDatFile);
            assert nbt != null;
            if (token.equals("")) {
                nbt.remove(serverIp);
            } else {
                nbt.putString(serverIp, token);
            }
            NbtIo.write(nbt, serversDatFile);
        } catch (Exception e) {
            //
        }
    }

    public static String getToken(String serverIp) {
        try {
            generateConfig();
            return Objects.requireNonNull(Objects.requireNonNull(NbtIo.read(serversDatFile)).get(serverIp)).asString();
        } catch (Exception e) {return null;}
    }
}