package com.admc.jcreole;

import beaver.Symbol;

/**
 * JCreole-specific scanner and parser symbol.
 */
class Token extends Symbol {
    public static final int UNSET = -1;
    private int offset = UNSET, line = UNSET, column = UNSET, intParam = UNSET;

    /* I keep vacillating about whether to store 0-based offsets of 1-based
     * offsets.  Sticking with the scanner-side convention for now.
     */
    public int getOffset() { return offset; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public int getIntParam() { return intParam; }

    public Token(short i) {
        this(i, null);
    }

    public Token(short i, String s) {
        super(i, s);
    }

    public Token(short i, String s,
            int offset, int line, int column, int intParam) {
        this(i, s);
        this.offset = offset;
        this.line = line;
        this.column = column;
        this.intParam = intParam;
    }

    public Token(short i, String s, int offset, int line, int column) {
        this(i, s, offset, line, column, UNSET);
    }

    public Token(short i, int offset, int line, int column) {
        this(i, null, offset, line, column, UNSET);
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
