package ru.niksne.packetauth.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AuthPayload(String token) implements CustomPayload {
    public static final CustomPayload.Id<AuthPayload> ID = new CustomPayload.Id<>(Identifier.of("packetauth", "auth"));

    public static final PacketCodec<RegistryByteBuf, AuthPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, AuthPayload::token,
            AuthPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
