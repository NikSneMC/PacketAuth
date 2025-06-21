package ru.niksne.packetauth.storage.file;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DisabledTokenStorage {
    static final String base = "disabled_tokens";

    @NotNull
    private final Map<@NotNull String, @NotNull String> disabled = new HashMap<>();

    @NotNull
    public Boolean isTokenDisabledFor(
            @NotNull
            String name
    ) {
        return disabled.containsKey(name);
    }

    @NotNull
    public String getTokenDisablingReason(
            @NotNull
            String name
    ) {
        return disabled.getOrDefault(name, "");
    }
}
