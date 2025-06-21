package ru.niksne.packetauth.fabric.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.jetbrains.annotations.NotNull;
import ru.niksne.packetauth.fabric.payload.AuthPayload;
import ru.niksne.packetauth.fabric.payload.TokenPayload;
import ru.niksne.packetauth.storage.file.StorageManager;
import ru.niksne.packetauth.storage.file.TokenStorage;

import java.util.Objects;

public class Handler implements ClientPlayNetworking.PlayPayloadHandler<TokenPayload>, ClientPlayConnectionEvents.Join {
    Handler() {
        PayloadTypeRegistry.playC2S().register(AuthPayload.ID, AuthPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TokenPayload.ID, TokenPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(TokenPayload.ID, this);

        ClientPlayConnectionEvents.JOIN.register(this);
    }

    @Override
    public void receive(
            @NotNull
            TokenPayload payload,
            @NotNull
            ClientPlayNetworking.Context context
    ) {
        @NotNull
        String ip;
        try (MinecraftClient client = context.client()) {
            ip = Objects.requireNonNull(client.getNetworkHandler()).getConnection().getAddress().toString();
        }
        ip = String.format("%s%s", ip.substring(0, ip.indexOf("/")), ip.substring(ip.indexOf(":")));

        StorageManager<TokenStorage> tokenStorageManager = PacketAuth.getTokenStorageManager();
        tokenStorageManager.getStorage().saveTokenFor(ip, payload.token());
        tokenStorageManager.saveStorage();
    }

    @Override
    public void onPlayReady(
            @NotNull
            ClientPlayNetworkHandler handler,
            @NotNull
            PacketSender sender,
            @NotNull
            MinecraftClient client
    ) {
        String ip = handler.getConnection().getAddress().toString();
        ip = String.format("%s%s", ip.substring(0, ip.indexOf("/")), ip.substring(ip.indexOf(":")));
        String token = PacketAuth.getTokenStorage().getTokenFor(ip).replace(";", "");
        ClientPlayNetworking.send(new AuthPayload("1.6;" + token));
    }
}
