package com.admc.jcreole;

/**
 * Exception detected by the JCreole system.
 * Note that in most cases you can use the getters for offset/line/column to
 * find out where the problem is the input.
 * <p>
 * Had to make this a RuntimeException.   If extend Beaver's Exception as they
 * intend, then am stuck with Beaver's fantastically incompetent error-handling.
 * </p> <p>
 * Note that the source locations reported are 0-based, not 1-based, and that
 * the reported numbers will be skewed (to higher/smaller locations) if your
 * input has carriage returns, because they are stripped out before the
 * offsets are counted.
 * <p>
 */
public class CreoleParseException extends RuntimeException {
    static final long serialVersionUID = 8504782268721814533L;

    private int offset, line, column;

    /* I keep vacillating about whether to store 0-based offsets of 1-based
     * offsets.  Sticking with the scanner-side convention for now.
     */
    public int getOffset() { return offset; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    public CreoleParseException(beaver.Parser.Exception e) {
        super(e);
    }

    public CreoleParseException(String msg, Token sourceToken) {
        this(msg, sourceToken.getOffset(),
                sourceToken.getLine(), sourceToken.getColumn());
    }

    public CreoleParseException(String msg, int offset, int line, int column) {
        super(String.format("%s.  @line:col %d:%d", msg, line + 1, column + 1));
        this.line = line;
        this.column = column;
        this.offset = offset;
    }
}
