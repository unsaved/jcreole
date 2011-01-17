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
    public Token(short i, Integer iger) {
        super(i, iger);
System.err.println("Intr");
    }
    public Token(short i, String s) {
        super(i, s);
System.err.println("Strer");
    }

    public String toString() {
        return "#" + getId() + "/(" + value + ')';
    }
}
