package com.niksne.packetauth.bungee;

import com.niksne.packetauth.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class PacketAuth extends Plugin implements Listener {
    private static ConfigManager config;
    private static ConfigManager tokens;
    private static ConfigManager disabled;

    private static MySQLManager db;

    private final Set<String> verified = new HashSet<>();
    private Set<String> outdated = new HashSet<>();

    @Override
    public void onEnable() {
        config = new ConfigManager(getDataFolder().getPath(), "config", "config");
        new MigrateConfig(config, tokens);

        getProxy().getPluginManager().registerListener(this, this);

        getProxy().registerChannel("packetauth:auth");
        getProxy().registerChannel("packetauth:token");

        Utils.checkAutogen(config);
        db = Utils.checkStorageType(config);

        if (db == null) tokens = new ConfigManager(getDataFolder().getPath(), "tokens", "empty");
        if (Utils.checkTokenDisabling(config, db)) disabled = new ConfigManager(getDataFolder().getPath(),"disabled_tokens", "empty");
    }

    @EventHandler
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (!event.getTag().equals("packetauth:auth")) return;

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        Utils.verify(event.getData(), outdated, player.getName(), config, tokens, verified);
    }

    @EventHandler
    public void onLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        LoginPreparer preparer = new LoginPreparer(config, db, outdated, player.getName(), player.getPing());
        outdated = preparer.getOutdated();
        db = preparer.getDb();
        preparer.getService().scheduleWithFixedDelay(
            () -> {
                preparer.getService().shutdown();
                if (player.isConnected()) {
                    LoginChecker checker = new LoginChecker(preparer, player.getName(), config, db, disabled, tokens, verified);
                    switch (checker.getAction()) {
                        case "kick" -> player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', checker.getReason())));
                        case "send_token" -> player.sendData("packetauth:token", checker.getToken().getBytes());
                        case "pass" -> verified.remove(player.getName());
                    }
                }
                outdated.remove(player.getName());
            }, preparer.getDelay(), preparer.getDelay(), TimeUnit.MILLISECONDS
        );
    }
}