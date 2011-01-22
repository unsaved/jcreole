package com.admc.jcreole;

import beaver.Symbol;

public class LiToken extends Token {
    private char type;
    private String content;

    public LiToken(Token sourceToken) {
        super(sourceToken.getId(), null,
                sourceToken.getOffset(), sourceToken.getLine(),
                sourceToken.getColumn(), sourceToken.getIntParam());
        String s = sourceToken.getStringVal();
        if (s == null) throw new NullPointerException();
        if (s.length() != 1)
            throw new IllegalArgumentException("Malformatted type: " + s);
        type = s.charAt(0);
    }

    public void setContent(String content) {
        this.content = content;
    }

    public char getType() {
        return type;
    }

    public int getLevel() {
        return getIntParam();
    }

    public String getContent() {
        return content;
    }

    public String toString() {
        return Character.toString(type) + getLevel()
                + ": <li level=\"" + getLevel() + "\">" + content + '>';
    }
}
