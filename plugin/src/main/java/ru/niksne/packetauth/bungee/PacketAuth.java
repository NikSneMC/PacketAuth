package ru.niksne.packetauth.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import ru.niksne.packetauth.storage.MigrateStorage;
import ru.niksne.packetauth.storage.ManagerWrapper;


public final class PacketAuth extends Plugin {
    @Override
    public void onEnable() {
        ManagerWrapper.init(getDataFolder().toPath());

        new MigrateStorage();
        new Handler(this);
    }
}