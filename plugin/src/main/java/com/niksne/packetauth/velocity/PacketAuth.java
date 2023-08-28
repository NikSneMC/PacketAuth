package com.niksne.packetauth.velocity;

import com.google.inject.Inject;
import com.niksne.packetauth.ConfigManager;
import com.niksne.packetauth.MigrateConfig;
import com.niksne.packetauth.MySQLManager;
import com.niksne.packetauth.Utils;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PacketAuth {
    private final ProxyServer proxy;
    private static ConfigManager config;
    private static ConfigManager tokens;

    private final Set<String> verified = new HashSet<>();
    private final Set<String> outdated = new HashSet<>();

    @Inject
    public PacketAuth(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        config = new ConfigManager("plugins/PacketAuth", "config", "config");
        new MigrateConfig(config, tokens);

        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("packetauth:auth"));
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("packetauth:token"));

        Utils.checkAutogen(config);

        if (Utils.checkStorageType(config, false) == null) tokens = new ConfigManager("plugins/PacketAuth", "tokens", "empty");

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
        outdated.add(player.getUsername());

        MySQLManager db = Utils.checkStorageType(config, true);
        boolean autogenEnabled = Utils.checkAutogen(config);

        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        long delay = (long) Utils.eval(config.getString("kick.delay").replace("%ping%", String.valueOf(player.getPing())));
        service.scheduleWithFixedDelay(
                () -> {
                    if (player.getCurrentServer().isPresent()) {
                        if (outdated.contains(player.getUsername())) player.disconnect(Component.text(config.getString("kick.outdated").replace("%version%", "1.6").replace("&", "§")));
                        else {
                            if (autogenEnabled && db == null && !tokens.containsKey(player.getUsername())) {
                                String token = Utils.generateRandomToken(config.getString("tokengen.symbols").replace(";", ""), Integer.parseInt(config.getString("tokengen.length")));
                                tokens.putString(player.getUsername(), token);
                                player.sendPluginMessage(MinecraftChannelIdentifier.from("packetauth:token"), token.getBytes());
                            } else if (autogenEnabled && db != null && db.noPlayer(player.getUsername())) {
                                String token = Utils.generateRandomToken(config.getString("tokengen.symbols").replace(";", ""), Integer.parseInt(config.getString("tokengen.length")));
                                db.saveToken(player.getUsername(), token);
                                player.sendPluginMessage(MinecraftChannelIdentifier.from("packetauth:token"), token.getBytes());
                            } else {
                                if (!verified.contains(player.getUsername())) player.disconnect(Component.text(config.getString("kick.message").replace("%name%", player.getUsername()).replace("&", "§")));
                                else verified.remove(player.getUsername());
                            }
                        }
                        outdated.remove(player.getUsername());
                    }
                    service.shutdown();
                }, delay, delay, TimeUnit.MILLISECONDS
        );
    }
}
