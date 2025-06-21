package ru.niksne.packetauth.storage.file;

import org.jetbrains.annotations.NotNull;
import ru.niksne.packetauth.storage.file.sections.*;

public class ConfigStorage {
    @NotNull
    static final String base = "config";

    @NotNull
    public ConfigInfo config = new ConfigInfo();
    @NotNull
    public KickSettings kick = new KickSettings();
    @NotNull
    public StorageSettings storage = new StorageSettings();
    @NotNull
    public TokenDisablingSettings token_disabling = new TokenDisablingSettings();
    @NotNull
    public TokenGenSettings tokengen = new TokenGenSettings();
}
