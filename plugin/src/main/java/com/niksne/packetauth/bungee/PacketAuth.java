package com.niksne.packetauth.bungee;

import com.niksne.packetauth.ConfigManager;
import com.niksne.packetauth.MigrateConfig;
import com.niksne.packetauth.MySQLManager;
import com.niksne.packetauth.Utils;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.niksne.packetauth.Utils.eval;

public final class PacketAuth extends Plugin implements Listener {
    private static ConfigManager config;
    private static ConfigManager tokens;

    private final Set<String> verified = new HashSet<>();
    private final Set<String> outdated = new HashSet<>();

    @Override
    public void onEnable() {
        config = new ConfigManager(getDataFolder().getPath(), "config", "config");
        new MigrateConfig(config, tokens);
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().registerChannel("packetauth:auth");
        getProxy().registerChannel("packetauth:token");

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
            tokens = new ConfigManager(getDataFolder().getPath(), "tokens", "empty");
            config.removeString("storage.mysql.host");
            config.removeString("storage.mysql.port");
            config.removeString("storage.mysql.databaseName");
            config.removeString("storage.mysql.tableName");
            config.removeString("storage.mysql.user");
            config.removeString("storage.mysql.password");
        }
    }

    @EventHandler
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (!event.getTag().equals("packetauth:auth")) return;

        ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();
        String income = new String(event.getData(), StandardCharsets.UTF_8);
        if (!income.contains(";")) income = "0;" + income;
        List<String> msg = List.of(income.split(";"));

        if (msg.get(0).compareTo("1.6") >= 0) outdated.remove(player.getName());

        if (config.getString("storage.mode").equals("mysql")) {
            MySQLManager db = new MySQLManager(
                    config.getString("storage.mysql.host"),
                    Integer.parseInt(config.getString("storage.mysql.port")),
                    config.getString("storage.mysql.databaseName"),
                    config.getString("storage.mysql.tableName"),
                    config.getString("storage.mysql.user"),
                    config.getString("storage.mysql.password")
            );
            if (!db.hasPlayer(player.getName())) return;
            if (msg.get(1).equals(db.getToken(player.getName()).replace(";", ""))) verified.add(player.getName());
        } else {
            if (!tokens.containsKey(player.getName())) return;
            if (msg.get(1).equals(tokens.getString(player.getName()).replace(";", ""))) verified.add(player.getName());
        }
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        ProxiedPlayer player = getProxy().getPlayer(event.getConnection().getName());
        outdated.add(player.getName());

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

        getProxy().getScheduler().schedule(this, () -> {
            if (player.getServer() != null) {
                if (outdated.contains(player.getName())) player.disconnect(config.getString("kick.outdated").replace("%version%", "1.6").replace("&", "§"));
                else {
                    if (autogenEnabled && !tokens.containsKey(player.getName())) {
                        String token = Utils.generateRandomToken(config.getString("tokengen.symbols").replace(";", ""), Integer.parseInt(config.getString("tokengen.length")));
                        tokens.putString(player.getName(), token);
                        player.sendData("packetauth:token", token.getBytes());
                    } else if (autogenEnabled && MySQLEnabled && !db.hasPlayer(player.getName())) {
                        String token = Utils.generateRandomToken(config.getString("tokengen.symbols").replace(";", ""), Integer.parseInt(config.getString("tokengen.length")));
                        db.saveToken(player.getName(), token);
                        player.sendData("packetauth:token", token.getBytes());
                    } else {
                        if (!verified.contains(player.getName())) player.disconnect(config.getString("kick.message").replace("%name%", player.getName()).replace("&", "§"));
                        else verified.remove(player.getName());
                    }
                }
                outdated.remove(player.getName());
            }
        }, (long) eval(config.getString("kick.delay").replace("%ping%", String.valueOf(player.getPing()))), TimeUnit.MILLISECONDS);
    }
}