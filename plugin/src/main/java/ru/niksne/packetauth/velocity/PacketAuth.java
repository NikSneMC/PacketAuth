package ru.niksne.packetauth.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import ru.niksne.packetauth.storage.MigrateStorage;
import ru.niksne.packetauth.storage.ManagerWrapper;

import java.nio.file.Path;

public class PacketAuth {
    private final ProxyServer proxy;
    private final Path pluginFolder;

    @Inject
    public PacketAuth(ProxyServer proxy, @DataDirectory Path pluginFolder) {
        this.proxy = proxy;
        this.pluginFolder = pluginFolder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ManagerWrapper.init(pluginFolder);

        new MigrateStorage();
        new Handler(proxy);
    }
}
