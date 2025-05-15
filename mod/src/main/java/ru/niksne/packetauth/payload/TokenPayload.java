package ru.niksne.packetauth.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TokenPayload(String token) implements CustomPayload {
    public static final CustomPayload.Id<TokenPayload> ID = new CustomPayload.Id<>(Identifier.of("packetauth", "token"));

    public static final PacketCodec<RegistryByteBuf, TokenPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, TokenPayload::token,
            TokenPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
