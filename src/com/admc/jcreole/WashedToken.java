package com.admc.jcreole;

import beaver.Symbol;

public class WashedToken extends Token {
    private final String cleanString;

    public WashedToken(String s) {
        cleanString = s;
    }

    public String toString() {
        return cleanString;
    }
}
