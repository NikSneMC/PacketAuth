package com.niksne.packetauth.client;

import com.niksne.packetauth.ConfigManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;

public class PacketAuth implements ClientModInitializer, ClientPlayNetworking.PlayChannelHandler {

	private static final Identifier AUTH_PACKET_ID = new Identifier("packetauth:auth");
	private static final Identifier AUTH_TOKEN = new Identifier("packetauth:token");
	private static final ConfigManager config = new ConfigManager(FabricLoader.getInstance().getGameDir() + "/config/PacketAuth","config", "empty");

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(AUTH_TOKEN, this);
		new MigrateConfig(config);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			String ip = handler.getConnection().getAddress().toString();
			String token = config.getString(String.format("%s%s", ip.substring(0, ip.indexOf("/")), ip.substring(ip.indexOf(":")))).replace(";", "");
			ClientPlayNetworking.send(PacketAuth.AUTH_PACKET_ID, new PacketByteBuf(Unpooled.wrappedBuffer(("1.6;" + token).getBytes())));
		});
	}

	@Override
	public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
		String ip = handler.getConnection().getAddress().toString();
		config.putString(String.format("%s%s", ip.substring(0, ip.indexOf("/")), ip.substring(ip.indexOf(":"))), new String(buf.copy().array(), StandardCharsets.UTF_8));
	}

	public static ConfigManager getConfig() { return config; }
}
