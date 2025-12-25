package ru.niksne.packetauth.utils;


import org.jetbrains.annotations.NotNull;

public class MathParser {
    @NotNull
    String expression;
    int pos = -1, ch;

    public MathParser(
        @NotNull
        String expression
    ) {
        this.expression = expression;
    }

    void nextChar() {
        ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
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

    public double parseDouble() {
        nextChar();
        double x = parseExpression();

        if (pos < expression.length()) {
            throw new RuntimeException("Unexpected: " + (char) ch);
        }

        return x;
    }

    public long parseLong() {
        return (long) this.parseDouble();
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

            x = Double.parseDouble(expression.substring(startPos, this.pos));
        } else {
            throw new RuntimeException("Unexpected: " + (char) ch);
        }

        return x;
    }
}
