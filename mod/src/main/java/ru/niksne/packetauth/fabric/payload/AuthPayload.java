package ru.niksne.packetauth.fabric.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import ru.niksne.packetauth.Channel;

// TODO: implement normal packets on plugin side, not just stupid semicolon-separated string
public record AuthPayload(
        @NotNull
        String token
) implements CustomPayload {
    @NotNull
    public static final CustomPayload.Id<AuthPayload> ID = new CustomPayload.Id<>(Identifier.of(Channel.namespace, Channel.suffix_c2s));

    @NotNull
    public static final PacketCodec<RegistryByteBuf, AuthPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, AuthPayload::token,
            AuthPayload::new
    );

    @Override
    @NotNull
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
