package com.niksne.packetauth.client;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import static com.niksne.packetauth.client.Utils.generateConfig;
import static com.niksne.packetauth.client.Utils.getToken;

public class PacketAuth implements ClientModInitializer {

	private static final Identifier AUTH_PACKET_ID = new Identifier("packetauth:auth");

	@Override
	public void onInitializeClient() {
		generateConfig();
		ClientPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			System.out.println(handler.getConnection().getAddress());
			for (String ip : handler.getConnection().getAddress().toString().split("/")) {
				String token = getToken(ip);
				if (token != null) {
					System.out.println("Sending token (" + token + ") to server!");
					ClientPlayNetworking.send(PacketAuth.AUTH_PACKET_ID, new PacketByteBuf(Unpooled.wrappedBuffer(token.getBytes())));
				}
			}
		});
	}
}
