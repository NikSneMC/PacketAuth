package com.niksne.packetauth.bungee;

import com.niksne.packetauth.ConfigManager;
import com.niksne.packetauth.MigrateConfig;
import com.niksne.packetauth.MySQLManager;
import com.niksne.packetauth.Utils;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

        Utils.checkAutogen(config);

        if (Utils.checkStorageType(config, false) == null)tokens = new ConfigManager(getDataFolder().getPath(), "tokens", "empty");

    }

    @EventHandler
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (!event.getTag().equals("packetauth:auth")) return;

        ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();

        Utils.verify(event.getData(), outdated, player.getName(), config, tokens, verified);
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        ProxiedPlayer player = getProxy().getPlayer(event.getConnection().getName());
        outdated.add(player.getName());

        MySQLManager db = Utils.checkStorageType(config, true);
        boolean autogenEnabled = Utils.checkAutogen(config);

        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        long delay = (long) Utils.eval(config.getString("kick.delay").replace("%ping%", String.valueOf(player.getPing())));
        service.scheduleWithFixedDelay(
                () -> {
                    if (outdated.contains(player.getName())) player.disconnect(new TextComponent(config.getString("kick.outdated").replace("%version%", "1.6").replace("&", "§")));
                    else {
                        if (autogenEnabled && db == null && !tokens.containsKey(player.getName())) {
                            String token = Utils.generateRandomToken(config.getString("tokengen.symbols").replace(";", ""), Integer.parseInt(config.getString("tokengen.length")));
                            tokens.putString(player.getName(), token);
                            player.sendData("packetauth:token", token.getBytes());
                        } else if (autogenEnabled && db != null && db.noPlayer(player.getName())) {
                            String token = Utils.generateRandomToken(config.getString("tokengen.symbols").replace(";", ""), Integer.parseInt(config.getString("tokengen.length")));
                            db.saveToken(player.getName(), token);
                            player.sendData("packetauth:token", token.getBytes());
                        } else {
                            if (!verified.contains(player.getName())) player.disconnect(new TextComponent(config.getString("kick.message").replace("%name%", player.getName()).replace("&", "§")));
                            else verified.remove(player.getName());
                        }
                    }
                    outdated.remove(player.getName());
                    service.shutdown();
                }, delay, delay, TimeUnit.MILLISECONDS
        );
    }
}