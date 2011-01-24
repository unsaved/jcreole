package com.admc.jcreole;

import beaver.Symbol;

/**
 * A Parser token specifically marked as being HTML-safe.
 */
class WashedToken extends Token {
    private final String cleanString;

    public WashedToken(String s) {
        cleanString = s;
    }

    public String toString() {
        return cleanString;
    }
}
