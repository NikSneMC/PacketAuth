package ru.niksne.packetauth.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import ru.niksne.packetauth.Channel;
import ru.niksne.packetauth.login.LoginCheckerAction;
import ru.niksne.packetauth.login.LoginFlow;
import ru.niksne.packetauth.login.LoginPreparation;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Handler implements Listener {
    @NotNull
    private final Set<@NotNull String> verified = new HashSet<>();
    @NotNull
    private Set<@NotNull String> outdated = new HashSet<>();

    public Handler(
        @NotNull
        PacketAuth packetAuth
    ) {

        packetAuth.getProxy().getPluginManager().registerListener(packetAuth, this);

        packetAuth.getProxy().registerChannel(Channel.c2s);
        packetAuth.getProxy().registerChannel(Channel.s2c);
    }

    @EventHandler
    public void onPluginMessageReceived(
        @NotNull
        PluginMessageEvent event
    ) {
        if (!event.getTag().equals(Channel.c2s)) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        LoginFlow.verify(
            event.getData(),
            outdated,
            player.getName(),
            verified
        );
    }

    @EventHandler
    public void onLogin(
        @NotNull
        PostLoginEvent event
    ) {
        ProxiedPlayer player = event.getPlayer();
        String playerName = player.getName();
        LoginPreparation preparation = LoginFlow.prepare(
            outdated,
            playerName,
            player.getPing()
        );

        try (ScheduledExecutorService service = preparation.service()) {
            service.schedule(
                () -> {
                    service.shutdown();
                    if (player.isConnected()) {
                        LoginCheckerAction action = LoginFlow.check(
                            outdated,
                            playerName,
                            verified
                        );

                        switch (action) {
                            case LoginCheckerAction.Kick verdict ->
                                player.disconnect(new TextComponent(ChatColor.translateAlternateColorCodes('&', verdict.reason())));
                            case LoginCheckerAction.SendToken verdict ->
                                player.sendData(Channel.s2c, verdict.token().getBytes());
                            case LoginCheckerAction.Pass ignored -> verified.remove(playerName);
                        }
                    }
                    outdated.remove(playerName);
                }, preparation.delay(), TimeUnit.MILLISECONDS
            );
        }   }
}
