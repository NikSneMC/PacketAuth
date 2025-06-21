package ru.niksne.packetauth.fabric.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import ru.niksne.packetauth.Channel;

public record TokenPayload(
        @NotNull
        String token
) implements CustomPayload {
    @NotNull
    public static final CustomPayload.Id<TokenPayload> ID = new CustomPayload.Id<>(Identifier.of(Channel.namespace, Channel.suffix_s2c));

    @NotNull
    public static final PacketCodec<RegistryByteBuf, TokenPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, TokenPayload::token,
            TokenPayload::new
    );

    @Override
    @NotNull
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
