package ru.niksne.packetauth.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import ru.niksne.packetauth.storage.MigrateStorage;
import ru.niksne.packetauth.storage.ManagerWrapper;

public final class PacketAuth extends JavaPlugin {
    private static boolean isFolia;

    @Override
    public void onEnable() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

        ManagerWrapper.init(getDataFolder().toPath());

        new MigrateStorage();
        new Handler(this);
    }

    public static boolean isFolia() {
        return isFolia;
    }
}
