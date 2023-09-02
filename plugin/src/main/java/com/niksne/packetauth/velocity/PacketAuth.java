package com.niksne.packetauth.velocity;

import com.google.inject.Inject;
import com.niksne.packetauth.*;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PacketAuth {
    private final ProxyServer proxy;
    private final Path pluginFolder;
    private static ConfigManager config;
    private static ConfigManager tokens;
    private static ConfigManager disabled;

    private static MySQLManager db;

    private final Set<String> verified = new HashSet<>();
    private Set<String> outdated = new HashSet<>();

    @Inject
    public PacketAuth(ProxyServer proxy, @DataDirectory Path pluginFolder) {
        this.proxy = proxy;
        this.pluginFolder = pluginFolder;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        config = new ConfigManager(pluginFolder.toString(), "config", "config");
        new MigrateConfig(config, tokens);

        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("packetauth:auth"));
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("packetauth:token"));

        Utils.checkAutogen(config);
        db = Utils.checkStorageType(config);

        if (db == null) tokens = new ConfigManager("plugins/PacketAuth", "tokens", "empty");
        if (Utils.checkTokenDisabling(config, db)) disabled = new ConfigManager("plugins/PacketAuth","disabled_tokens", "empty");
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getIdentifier().getId().equals("packetauth:auth")) return;
        if (!(e.getSource() instanceof Player player)) return;

        Utils.verify(e.getData(), outdated, player.getUsername(), config, tokens, verified);
    }

    @Subscribe
    public void onLogin(LoginEvent e) {
        Player player = e.getPlayer();
        LoginPreparer preparer = new LoginPreparer(config, db, outdated, player.getUsername(), player.getPing());
        outdated = preparer.getOutdated();
        db = preparer.getDb();
        preparer.getService().scheduleWithFixedDelay(
            () -> {
                preparer.getService().shutdown();
                if (player.isActive()) {
                    LoginChecker checker = new LoginChecker(preparer, player.getUsername(), config, db, disabled, tokens, verified);
                    switch (checker.getAction()) {
                        case "kick" -> player.disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(checker.getReason()));
                        case "send_token" -> player.sendPluginMessage(MinecraftChannelIdentifier.from("packetauth:token"), checker.getToken().getBytes());
                        case "pass" -> verified.remove(player.getUsername());
                    }
                }
                outdated.remove(player.getUsername());
            }, preparer.getDelay(), preparer.getDelay(), TimeUnit.MILLISECONDS
        );
    }
}
