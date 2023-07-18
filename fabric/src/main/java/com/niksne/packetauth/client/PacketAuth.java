package com.niksne.packetauth.client;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.Arrays;

import static com.niksne.packetauth.client.Utils.generateConfig;
import static com.niksne.packetauth.client.Utils.getToken;

public class PacketAuth implements ClientModInitializer {

	private static final Identifier AUTH_PACKET_ID = new Identifier("packetauth:auth");

	@Override
	public void onInitializeClient() {
		generateConfig();
		ClientPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			String ip = handler.getConnection().getAddress().toString();
			String token = getToken(ip.substring(0, ip.lastIndexOf(':')).substring(0, ip.indexOf("/")));
			if (token == null) return;
			ClientPlayNetworking.send(PacketAuth.AUTH_PACKET_ID, new PacketByteBuf(Unpooled.wrappedBuffer(token.getBytes())));
		});
	}
}
