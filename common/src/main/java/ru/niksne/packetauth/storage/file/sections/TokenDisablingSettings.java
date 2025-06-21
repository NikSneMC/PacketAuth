package ru.niksne.packetauth.storage.file.sections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TokenDisablingSettings {
    @NotNull
    public Boolean enabled = true;
    @Nullable
    public String table_name = "disabledTokens";
}
