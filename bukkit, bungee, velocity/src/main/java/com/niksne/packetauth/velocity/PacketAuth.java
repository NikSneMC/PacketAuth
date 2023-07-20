package com.niksne.packetauth.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.niksne.packetauth.Utils.eval;

@Plugin(
        id = "packetauth",
        name = "Packet Auth",
        version = "1.5.1",
        description = "Authorization system for servers without online mode",
        url = "https://modrinth.com/mod/packetauth",
        authors = {"NikSne"}
)
public class PacketAuth {
    private final Set<String> verified = new HashSet<>();
    private final ProxyServer proxy;
    public ConfigManager config;

    @Inject
    public PacketAuth(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        config = new ConfigManager("config.yml");
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("packetauth:auth"));
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getIdentifier().getId().equals("packetauth:auth")) return;
        if (!(e.getSource() instanceof Player player)) return;

        String token = config.getString(player.getUsername());
        if (token == null) return;
        if (new String(e.getData(), StandardCharsets.UTF_8).equals(token)) verified.add(player.getUsername());
    }

    @Subscribe
    public void onLogin(LoginEvent e) {
        Player player = e.getPlayer();
        proxy.getScheduler().buildTask(this, () -> {
            if (
                    proxy.getPlayer(player.getUniqueId()).isPresent() && !verified.contains(player.getUsername())
            ) player.getCurrentServer().ifPresent(connection -> player.disconnect(
                        Component.text(config.getString("kick.message").replace(
                                "%server%",
                                connection.getServer().getServerInfo().getName()
                        ))
                ));
            else verified.remove(player.getUsername());
        }).delay((long) eval(config.getString("kick.delay")
                .replace("%ping%", String.valueOf(player.getPing()))), TimeUnit.MILLISECONDS).schedule();
    }
}
