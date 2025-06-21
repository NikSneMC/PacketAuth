package ru.niksne.packetauth.storage.file.sections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TokenGenSettings {
    @NotNull
    public Boolean enabled = true;
    @Nullable
    public Integer length = 4096;
    @Nullable
    public String symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
}
