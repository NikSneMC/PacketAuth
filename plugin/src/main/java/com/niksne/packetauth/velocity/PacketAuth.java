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

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.niksne.packetauth.Utils.eval;

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

        if (Boolean.parseBoolean(config.getString("tokengen.enabled"))) {
            config.addString("tokengen.length", "4096");
            config.addString("tokengen.symbols", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        } else {
            config.removeString("tokengen.length");
            config.removeString("tokengen.symbols");
        }

        if (config.getString("storage.mode").equals("mysql")) {
            config.addString("storage.mysql.host", "localhost");
            config.addString("storage.mysql.port", "3306");
            config.addString("storage.mysql.databaseName", "PacketAuth");
            config.addString("storage.mysql.tableName", "Tokens");
            config.addString("storage.mysql.user", "PacketAuth");
            config.addString("storage.mysql.password", "PacketAuthPluginPassword1234");
        } else {
            tokens = new ConfigManager("plugins/PacketAuth", "tokens", "empty");
            config.removeString("storage.mysql.host");
            config.removeString("storage.mysql.port");
            config.removeString("storage.mysql.databaseName");
            config.removeString("storage.mysql.tableName");
            config.removeString("storage.mysql.user");
            config.removeString("storage.mysql.password");
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getIdentifier().getId().equals("packetauth:auth")) return;
        if (!(e.getSource() instanceof Player player)) return;

        String income = new String(e.getData(), StandardCharsets.UTF_8);
        if (!income.contains(";")) income = "0;" + income;
        List<String> msg = List.of(income.split(";"));

        if (msg.get(0).compareTo("1.6") >= 0) outdated.remove(player.getUsername());

        if (config.getString("storage.mode").equals("mysql")) {
            MySQLManager db = new MySQLManager(
                    config.getString("storage.mysql.host"),
                    Integer.parseInt(config.getString("storage.mysql.port")),
                    config.getString("storage.mysql.databaseName"),
                    config.getString("storage.mysql.tableName"),
                    config.getString("storage.mysql.user"),
                    config.getString("storage.mysql.password")
            );
            if (!db.hasPlayer(player.getUsername())) return;
            if (msg.get(1).equals(db.getToken(player.getUsername()).replace(";", ""))) verified.add(player.getUsername());
        } else {
            if (!tokens.containsKey(player.getUsername())) return;
            if (msg.get(1).equals(tokens.getString(player.getUsername()).replace(";", "")))
                verified.add(player.getUsername());
        }
    }

    @Subscribe
    public void onLogin(LoginEvent e) {
        Player player = e.getPlayer();
        outdated.add(player.getUsername());

        boolean autogenEnabled = Boolean.parseBoolean(config.getString("tokengen.enabled"));
        if (autogenEnabled) {
            config.addString("tokengen.length", "4096");
            config.addString("tokengen.symbols", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        } else {
            config.removeString("tokengen.length");
            config.removeString("tokengen.symbols");
        }

        boolean MySQLEnabled = config.getString("storage.mode").equals("mysql");
        MySQLManager db;
        if (MySQLEnabled) {
            config.addString("storage.mysql.host", "localhost");
            config.addString("storage.mysql.port", "3306");
            config.addString("storage.mysql.databaseName", "PacketAuth");
            config.addString("storage.mysql.tableName", "Tokens");
            config.addString("storage.mysql.user", "PacketAuth");
            config.addString("storage.mysql.password", "PacketAuthPluginPassword1234");
            db = new MySQLManager(
                    config.getString("storage.mysql.host"),
                    Integer.parseInt(config.getString("storage.mysql.port")),
                    config.getString("storage.mysql.databaseName"),
                    config.getString("storage.mysql.tableName"),
                    config.getString("storage.mysql.user"),
                    config.getString("storage.mysql.password")
            );
        } else {
            db = null;
            config.removeString("storage.mysql.host");
            config.removeString("storage.mysql.port");
            config.removeString("storage.mysql.databaseName");
            config.removeString("storage.mysql.tableName");
            config.removeString("storage.mysql.user");
            config.removeString("storage.mysql.password");
        }

        proxy.getScheduler().buildTask(this, () -> {
            if (player.getCurrentServer().isPresent()) {
                if (outdated.contains(player.getUsername())) player.disconnect(Component.text(config.getString("kick.outdated").replace("%version%", "1.6").replace("&", "§")));
                else {
                    if (autogenEnabled && !tokens.containsKey(player.getUsername())) {
                        String token = Utils.generateRandomToken(config.getString("tokengen.symbols").replace(";", ""), Integer.parseInt(config.getString("tokengen.length")));
                        tokens.putString(player.getUsername(), token);
                        player.sendPluginMessage(MinecraftChannelIdentifier.from("packetauth:token"), token.getBytes());
                    } else if (autogenEnabled && MySQLEnabled && !db.hasPlayer(player.getUsername())) {
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
        }).delay((long) eval(config.getString("kick.delay")
                .replace("%ping%", String.valueOf(player.getPing()))), TimeUnit.MILLISECONDS).schedule();
    }
}
