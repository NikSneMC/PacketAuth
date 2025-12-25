package ru.niksne.packetauth.storage.file.sections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;

public class TokenGenSettings {
    public boolean enabled = true;
    @Nullable
    public Integer length = 4096;
    @Nullable
    public String symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";


    @NotNull
    public String generateRandomToken() {
        assert this.symbols != null && this.length != null;

        String sourceString = this.symbols.replace(";", "");
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < this.length; i++) {
            int randomIndex = random.nextInt(sourceString.length());
            char randomChar = sourceString.charAt(randomIndex);
            sb.append(randomChar);
        }

        return sb.toString();
    }
}
