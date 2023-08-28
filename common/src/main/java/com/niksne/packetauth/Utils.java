package com.niksne.packetauth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

public class Utils {
    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    if (!eat(')')) throw new RuntimeException("Missing ')'");
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    if (eat('(')) {
                        x = parseExpression();
                        if (!eat(')')) throw new RuntimeException("Missing ')' after argument to " + func);
                    } else {
                        x = parseFactor();
                    }
                    x = switch (func) {
                        case "sqrt" -> Math.sqrt(x);
                        case "sin" -> Math.sin(Math.toRadians(x));
                        case "cos" -> Math.cos(Math.toRadians(x));
                        case "tan" -> Math.tan(Math.toRadians(x));
                        default -> throw new RuntimeException("Unknown function: " + func);
                    };
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }
    public static String generateRandomToken(String sourceString, int length) {
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(sourceString.length());
            char randomChar = sourceString.charAt(randomIndex);
            sb.append(randomChar);
        }

        return sb.toString();
    }

    public static boolean checkAutogen(ConfigManager config) {
        boolean autogenEnabled = Boolean.parseBoolean(config.getString("tokengen.enabled"));
        if (autogenEnabled) {
            config.addString("tokengen.length", "4096");
            config.addString("tokengen.symbols", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        } else {
            config.removeString("tokengen.length");
            config.removeString("tokengen.symbols");
        }
        return autogenEnabled;
    }

    public static MySQLManager checkStorageType(ConfigManager config, boolean connect) {
        boolean MySQLEnabled = config.getString("storage.mode").equals("mysql");
        MySQLManager db;
        if (MySQLEnabled) {
            config.addString("storage.mysql.host", "localhost");
            config.addString("storage.mysql.port", "3306");
            config.addString("storage.mysql.databaseName", "PacketAuth");
            config.addString("storage.mysql.tableName", "Tokens");
            config.addString("storage.mysql.user", "PacketAuth");
            config.addString("storage.mysql.password", "PacketAuthPluginPassword1234");
            if (connect) db = new MySQLManager(
                        config.getString("storage.mysql.host"),
                        Integer.parseInt(config.getString("storage.mysql.port")),
                        config.getString("storage.mysql.databaseName"),
                        config.getString("storage.mysql.tableName"),
                        config.getString("storage.mysql.user"),
                        config.getString("storage.mysql.password")
                );
            else db = null;
        } else {
            db = null;
            config.removeString("storage.mysql.host");
            config.removeString("storage.mysql.port");
            config.removeString("storage.mysql.databaseName");
            config.removeString("storage.mysql.tableName");
            config.removeString("storage.mysql.user");
            config.removeString("storage.mysql.password");
        }
        return db;
    }

    public static void verify(
            byte[] input,
            Set<String> outdated,
            String name,
            ConfigManager config,
            ConfigManager tokens,
            Set<String> verified
    ) {
        String income = new String(input, StandardCharsets.UTF_8);
        if (!income.contains(";")) income = "0;" + income;
        List<String> msg =  List.of(income.split(";"));
        if (msg.get(0).compareTo("1.6") >= 0) outdated.remove(name);
        MySQLManager db = Utils.checkStorageType(config, true);
        if (db == null) {
            if (!tokens.containsKey(name)) return;
            if (msg.get(1).equals(tokens.getString(name).replace(";", ""))) verified.add(name);
        } else {
            if (db.noPlayer(name)) return;
            if (msg.get(1).equals(db.getToken(name).replace(";", ""))) verified.add(name);
        }
    }
}
