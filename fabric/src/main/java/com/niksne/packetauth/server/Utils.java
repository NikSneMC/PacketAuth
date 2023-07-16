package com.niksne.packetauth.server;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.sql.*;
import java.util.Objects;

public class Utils {
    private static final File serversDatFile = new File(FabricLoader.getInstance().getGameDir() + "/settings.dat");
    public static void generateConfig() {
        try {
            if (!serversDatFile.exists()) {
                boolean success = serversDatFile.createNewFile();
                if (!success) { return; }
                NbtCompound config = new NbtCompound();
                config.putString("kick-delay", "%ping% + 110");
                config.putString("kick-message", "Authorization Error!");
                config.put("tokens", new NbtCompound());
                NbtIo.write(config, serversDatFile);
            }
        } catch (Exception e) {
            //
        }

    }
    public static String getKickDelay() {
        try {
            generateConfig();
            return Objects.requireNonNull(
                    Objects.requireNonNull(
                            NbtIo.read(serversDatFile)
                    ).get("kick-delay")
            ).asString();
        } catch (Exception e) {return "0";}
    }
    public static String getKickMsg() {
        try {
            generateConfig();
            return Objects.requireNonNull(
                    Objects.requireNonNull(
                            NbtIo.read(serversDatFile)
                    ).get("kick-message")
            ).asString();
        } catch (Exception e) {return "Disconnected";}
    }
    public static String getToken(String playerName) {
        try {
            generateConfig();
            return Objects.requireNonNull(
                    Objects.requireNonNull(
                            (NbtCompound) Objects.requireNonNull(
                                    NbtIo.read(serversDatFile)
                            ).get("tokens")
                    ).get(playerName)
            ).asString();
        } catch (Exception e) {return null;}
    }

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
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }
}