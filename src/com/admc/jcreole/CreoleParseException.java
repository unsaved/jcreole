package com.admc.jcreole;

public class CreoleParseException extends beaver.Scanner.Exception {
    static final long serialVersionUID = 8504782268721814533L;

    private int offset;

    /* I keep vacillating about whether to store 0-based offsets of 1-based
     * offsets.  Sticking with the scanner-side convention for now.
     */
    public int getOffset() { return offset; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    public CreoleParseException(String msg, Token sourceToken) {
        this(msg, sourceToken.getOffset(),
                sourceToken.getLine(), sourceToken.getColumn());
    }

    public CreoleParseException(String msg, int offset, int line, int column) {
        super(line, column, msg);
        this.offset = offset;
    }

    public String getMessage() {
        return super.getMessage() + " @line:col " + (line+1) + ':' + (column+1);
    }
}
