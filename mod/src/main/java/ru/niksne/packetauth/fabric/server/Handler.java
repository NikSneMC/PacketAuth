package ru.niksne.packetauth.fabric.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import ru.niksne.packetauth.Utils;
import ru.niksne.packetauth.fabric.payload.AuthPayload;
import ru.niksne.packetauth.fabric.payload.TokenPayload;
import ru.niksne.packetauth.login.LoginCheckerAction;
import ru.niksne.packetauth.login.LoginFlow;
import ru.niksne.packetauth.login.LoginPreparation;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Handler implements ServerPlayNetworking.PlayPayloadHandler<AuthPayload>, ServerPlayConnectionEvents.Join {
    @NotNull
    private final Set<@NotNull String> outdated = new HashSet<>();
    @NotNull
    private final Set<@NotNull String> verified = new HashSet<>();

    Handler() {
        PayloadTypeRegistry.playC2S().register(AuthPayload.ID, AuthPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TokenPayload.ID, TokenPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AuthPayload.ID, this);

        ServerPlayConnectionEvents.JOIN.register(this);
    }

    @Override
    public void receive(
            @NotNull
            AuthPayload payload,
            @NotNull
            ServerPlayNetworking.Context context
    ) {
        Utils.verify(
                payload.token().getBytes(),
                outdated,
                context.player().getName().getString(),
                verified
        );
    }

    @Override
    public void onPlayReady(
            @NotNull
            ServerPlayNetworkHandler handler,
            @NotNull
            PacketSender sender,
            @NotNull
            MinecraftServer server
    ) {
        ServerPlayerEntity player = handler.getPlayer();
        LoginPreparation preparation = LoginFlow.prepare(
                outdated,
                player.getName().getString(),
                (long) handler.getLatency()
        );

        preparation.service().scheduleWithFixedDelay(
                () -> {
                    preparation.service().shutdown();
                    if (!player.isDisconnected()) {
                        LoginCheckerAction action = LoginFlow.check(
                                outdated,
                                player.getName().getString(),
                                verified
                        );

                        switch (action) {
                            case LoginCheckerAction.Kick verdict ->
                                    player.networkHandler.disconnect(Text.of(verdict.reason().replace("&", "§")));
                            case LoginCheckerAction.SendToken verdict ->
                                    ServerPlayNetworking.send(player, new TokenPayload(verdict.token()));
                            case LoginCheckerAction.Pass ignored -> verified.remove(player.getName().getString());
                        }
                    }
                    outdated.remove(player.getName().getString());
                }, preparation.delay(), preparation.delay(), TimeUnit.MILLISECONDS
        );
    }
}
