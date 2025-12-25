package ru.niksne.packetauth;

import org.jetbrains.annotations.NotNull;

public class Channel {
    @NotNull
    public static final String namespace = "packetauth";

    @NotNull
    public static final String suffix_c2s = "auth";
    @NotNull
    public static final String suffix_s2c = "token";

    @NotNull
    public static final String c2s = String.format("%s:%s", namespace, suffix_c2s).trim();
    @NotNull
    public static final String s2c = String.format("%s:%s", namespace, suffix_s2c).trim();
}
