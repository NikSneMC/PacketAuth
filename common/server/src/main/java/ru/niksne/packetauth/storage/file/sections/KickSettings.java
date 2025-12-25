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



    @NotNull
    public String parseMessage(
        @NotNull
        String name,
        @NotNull
        String reason
    ) {
        String msg = this.disabled;
        assert msg != null;
        if (!reason.isBlank()) {
            msg += String.format("\n&r&f%s", this.disabled_reason);
        }
        return msg
            .replace("%name%", name)
            .replace("%reason%", String.format("&r&f%s", reason));
    }
}
