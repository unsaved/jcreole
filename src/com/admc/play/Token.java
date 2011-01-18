package com.admc.play;

import beaver.Symbol;

public class Token extends Symbol {
    public Token(short i, Object o) {
        super(i, o);
        throw new RuntimeException(
                "Unsupported Token value type: " + o.getClass().getName());
    }
    public Token(short i) {
        super(i);
    }
    public Token(short i, Character c) {
        super(i, c);
    }
    public Token(short i, Integer iger) {
        super(i, iger);
    }
    public Token(short i, String s) {
        super(i, s);
    }

    public String toString() {
        return "#" + getId() + "/(" + value + ')';
    }

    @SuppressWarnings("@unchecked")
    public String getStringVal() {
        return (String) value;
    }

    /**
     * A pass-through to beaver.Symbols is required for Beaver non-typed
     * Symbols.
     * That is a poorly designed Beaver idiocy where non-terminal Symbols are
     * created with hidden type values outside of the Terminals API.
     */
    protected Token() {
        // Intentionally empty
    }
}
