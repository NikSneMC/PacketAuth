package ru.niksne.packetauth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.niksne.packetauth.storage.ManagerWrapper;
import ru.niksne.packetauth.storage.database.DatabaseManager;
import ru.niksne.packetauth.storage.file.ConfigStorage;
import ru.niksne.packetauth.storage.file.TokenStorage;
import ru.niksne.packetauth.storage.file.sections.KickSettings;
import ru.niksne.packetauth.storage.file.sections.StorageSettings;
import ru.niksne.packetauth.storage.StorageType;
import ru.niksne.packetauth.storage.file.sections.TokenGenSettings;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Utils {
    public static double eval(
            @NotNull final String str
    ) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') {
                    nextChar();
                }

                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();

                if (pos < str.length()) {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) {
                        x += parseTerm(); // addition
                    } else if (eat('-')) {
                        x -= parseTerm(); // subtraction
                    } else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*')) {
                        x *= parseFactor(); // multiplication
                    } else if (eat('/')) {
                        x /= parseFactor(); // division
                    } else {
                        return x;
                    }
                }
            }

            double parseFactor() {
                if (eat('+')) {
                    return +parseFactor(); // unary plus
                }

                if (eat('-')) {
                    return -parseFactor(); // unary minus
                }

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();

                    if (!eat(')')) {
                        throw new RuntimeException("Missing ')'");
                    }
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') {
                        nextChar();
                    }

                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                return x;
            }
        }.parse();
    }

    @NotNull
    public static String generateRandomToken(
            @NotNull
            TokenGenSettings tokengen
    ) {
        assert tokengen.symbols != null && tokengen.length != null;

        String sourceString = tokengen.symbols.replace(";", "");
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < tokengen.length; i++) {
            int randomIndex = random.nextInt(sourceString.length());
            char randomChar = sourceString.charAt(randomIndex);
            sb.append(randomChar);
        }

        return sb.toString();
    }

    @NotNull
    public static Boolean checkTokenDisabling(
            @NotNull
            ConfigStorage configStorage,
            @Nullable
            DatabaseManager databaseManager
    ) {
        boolean tokenDisablingEnabled = configStorage.token_disabling.enabled;

        if (tokenDisablingEnabled && configStorage.storage.mode.equals(StorageType.MySQL) && databaseManager != null) {
            assert configStorage.token_disabling.table_name != null;
            databaseManager.createTable(configStorage.token_disabling.table_name, "reason");
        }

        return tokenDisablingEnabled;
    }

    @Nullable
    public static DatabaseManager checkStorageType(StorageSettings storage) {
        if (storage.mode == StorageType.File) {
            return null;
        }

        assert storage.host != null
                && storage.port != null
                && storage.database_name != null
                && storage.table_name != null
                && storage.user != null
                && storage.password != null;

        return new DatabaseManager(
                storage.mode,
                storage.host,
                storage.port,
                storage.database_name,
                storage.table_name,
                storage.user,
                storage.password
        );
    }

    public static void verify(
            byte[] input,
            @NotNull
            Set<String> outdated,
            @NotNull
            String name,
            @NotNull
            Set<String> verified
    ) {
        @NotNull
        StorageSettings storage = ManagerWrapper.getConfigStorage().storage;
        @Nullable
        TokenStorage tokenStorage = ManagerWrapper.getTokenStorage();

        String income = new String(input, StandardCharsets.UTF_8);
        if (!income.contains(";")) {
            income = "0;" + income;
        }

        List<String> msg = new java.util.ArrayList<>(List.of(income.split(";")));
        if (msg.size() == 1) {
            msg.add("");
        }
        if (msg.get(0).compareTo("1.6") >= 0) {
            outdated.remove(name);
        }

        DatabaseManager db = Utils.checkStorageType(storage);
        if (tokenStorage != null) {
            if (!tokenStorage.hasTokenFor(name)) {
                return;
            }

            if (msg.get(1).equals(tokenStorage.getTokenFor(name).replace(";", ""))) {
                verified.add(name);
            }
        } else if (db != null) {
            assert storage.table_name != null;
            if (!db.hasRecord(storage.table_name, "name", name)) {
                return;
            }

            if (msg.get(1).equals(Objects.requireNonNull(db.getToken(name)).replace(";", ""))) {
                verified.add(name);
            }
        }
    }

    @NotNull
    public static String parseMessage(
            @NotNull
            KickSettings kick,
            @NotNull
            String name,
            @NotNull
            String reason
    ) {
        String msg = kick.disabled;
        assert msg != null;
        if (!reason.isBlank()) {
            msg += String.format("\n&r&f%s", kick.disabled_reason);
        }
        return msg
                .replace("%name%", name)
                .replace("%reason%", String.format("&r&f%s", reason));
    }
}
