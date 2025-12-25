package ru.niksne.packetauth.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import ru.niksne.packetauth.Channel;
import ru.niksne.packetauth.login.LoginCheckerAction;
import ru.niksne.packetauth.login.LoginFlow;
import ru.niksne.packetauth.login.LoginPreparation;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Handler implements Listener, PluginMessageListener {
    @NotNull
    private final PacketAuth packetAuth;

    @NotNull
    private final Set<@NotNull String> verified = new HashSet<>();
    @NotNull
    private final Set<@NotNull String> outdated = new HashSet<>();

    Handler(
        @NotNull
        PacketAuth packetAuth
    ) {
        this.packetAuth = packetAuth;

        packetAuth.getServer().getPluginManager().registerEvents(this, packetAuth);

        Bukkit.getMessenger().registerIncomingPluginChannel(packetAuth, Channel.c2s, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(packetAuth, Channel.s2c);
    }

    @Override
    public void onPluginMessageReceived(
        @NotNull
        String channel,
        @NotNull
        Player player,
        byte[] message
    ) {
        if (!channel.equals(Channel.c2s)) {
            return;
        }

        LoginFlow.verify(
            message,
            outdated,
            player.getName(),
            verified
        );
    }

    @EventHandler
    public void onPlayerLogin(
        @NotNull
        PlayerLoginEvent event
    ) {
        Player player = event.getPlayer();
        LoginPreparation preparation = LoginFlow.prepare(
            outdated,
            player.getName(),
            player.getPing()
        );

        if (PacketAuth.isFolia()) {
            try (ScheduledExecutorService service = preparation.service()) {
                service.schedule(
                    () -> {
                        service.shutdown();
                        check(player);
                    }, preparation.delay(), TimeUnit.MILLISECONDS
                );
            }
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                check(player);
            }
        }.runTaskLaterAsynchronously(packetAuth, preparation.delay() / 20);
    }

    private void check(
        @NotNull
        Player player
    ) {
        if (player.isOnline()) {
            LoginCheckerAction action = LoginFlow.check(
                outdated,
                player.getName(),
                verified
            );

            switch (action) {
                case LoginCheckerAction.Kick verdict -> player.kickPlayer(ChatColor.translateAlternateColorCodes('&', verdict.reason()));
                case LoginCheckerAction.SendToken verdict -> player.sendPluginMessage(packetAuth, Channel.s2c, verdict.token().getBytes());
                case LoginCheckerAction.Pass ignored -> verified.remove(player.getName());
            }
        }
        outdated.remove(player.getName());
    }
}
