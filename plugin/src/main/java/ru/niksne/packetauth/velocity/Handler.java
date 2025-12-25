package ru.niksne.packetauth.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import ru.niksne.packetauth.Channel;
import ru.niksne.packetauth.login.LoginCheckerAction;
import ru.niksne.packetauth.login.LoginFlow;
import ru.niksne.packetauth.login.LoginPreparation;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Handler {
    @NotNull
    private final Set<@NotNull String> verified = new HashSet<>();
    @NotNull
    private Set<@NotNull String> outdated = new HashSet<>();

    Handler(
        @NotNull
        ProxyServer proxy
    ) {
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.create(Channel.namespace, Channel.c2s));
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.create(Channel.namespace, Channel.s2c));
    }

    @Subscribe
    public void onPluginMessage(
        @NotNull
        PluginMessageEvent event
    ) {
        if (!event.getIdentifier().getId().equals(Channel.c2s)) {
            return;
        }
        if (!(event.getSource() instanceof Player player)) {
            return;
        }

        LoginFlow.verify(
            event.getData(),
            outdated,
            player.getUsername(),
            verified
        );
    }

    @Subscribe
    public void onLogin(
        @NotNull
        LoginEvent event
    ) {
        Player player = event.getPlayer();
        String playerName = player.getUsername();
        LoginPreparation preparation = LoginFlow.prepare(
            outdated,
            playerName,
            player.getPing()
        );

        try (ScheduledExecutorService service = preparation.service()) {
            service.schedule(
                () -> {
                    service.shutdown();
                    if (player.isActive()) {
                        LoginCheckerAction action = LoginFlow.check(
                            outdated,
                            playerName,
                            verified
                        );

                        switch (action) {
                            case LoginCheckerAction.Kick verdict ->
                                player.disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(verdict.reason()));
                            case LoginCheckerAction.SendToken verdict ->
                                player.sendPluginMessage(MinecraftChannelIdentifier.from(Channel.s2c), verdict.token().getBytes());
                            case LoginCheckerAction.Pass ignored -> verified.remove(playerName);
                        }
                    }
                    outdated.remove(playerName);
                }, preparation.delay(), TimeUnit.MILLISECONDS
            );
        }
    }
}
