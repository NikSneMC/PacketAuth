package ru.niksne.packetauth.client;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import ru.niksne.packetauth.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import ru.niksne.packetauth.payload.AuthPayload;
import ru.niksne.packetauth.payload.TokenPayload;

import java.util.Objects;

public class PacketAuth implements ClientModInitializer, ClientPlayNetworking.PlayPayloadHandler<TokenPayload> {

	private static final ConfigManager config = new ConfigManager(FabricLoader.getInstance().getGameDir() + "/config/PacketAuth","config", "empty");

	@Override
	public void onInitializeClient() {
		PayloadTypeRegistry.playC2S().register(AuthPayload.ID, AuthPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TokenPayload.ID, TokenPayload.CODEC);
		ClientPlayNetworking.registerGlobalReceiver(TokenPayload.ID, this);

		new MigrateConfig(config);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			String ip = handler.getConnection().getAddress().toString();
			String token = config.getString(String.format("%s%s", ip.substring(0, ip.indexOf("/")), ip.substring(ip.indexOf(":")))).replace(";", "");
			ClientPlayNetworking.send(new AuthPayload("1.6;" + token));
		});
	}

	@Override
	public void receive(TokenPayload payload, ClientPlayNetworking.Context context) {
		String ip;
		try (MinecraftClient client = context.client()){
			ip = Objects.requireNonNull(client.getNetworkHandler()).getConnection().getAddress().toString();
		}
		config.putString(String.format("%s%s", ip.substring(0, ip.indexOf("/")), ip.substring(ip.indexOf(":"))), payload.token());
	}

	public static ConfigManager getConfig() { return config; }
}
