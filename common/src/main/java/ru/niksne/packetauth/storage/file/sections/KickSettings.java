package ru.niksne.packetauth.storage.file.sections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KickSettings {
    @NotNull
    public String outdated = "&cYou need &aPacket Auth %version% or never &cto play on this server!";
    @NotNull
    public String message = "&cAuthorization error!";
    @NotNull
    public String delay = "%ping% + 200";
    @Nullable
    public String disabled = "&cYour token has been disabled!";
    @Nullable
    public String disabled_reason = "&cReason: %reason%";
}
