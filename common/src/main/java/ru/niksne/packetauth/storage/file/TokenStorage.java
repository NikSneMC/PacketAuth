package ru.niksne.packetauth.storage.file;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TokenStorage {
    @NotNull
    static final String base = "tokens";

    @NotNull
    private Map<@NotNull String, @NotNull String> tokens = new HashMap<>();

    @NotNull
    public Boolean hasTokenFor(
            @NotNull
            String name
    ) {
        return tokens.containsKey(name);
    }

    @NotNull
    public String getTokenFor(
            @NotNull
            String name
    ) {
        return tokens.getOrDefault(name, "");
    }

    public void saveTokenFor(
            @NotNull
            String name,
            @NotNull
            String token
    ) {
        tokens.put(name, token);
    }
}

