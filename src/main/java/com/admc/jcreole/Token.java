/*
 * Copyright 2011 Axis Data Management Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.admc.jcreole;

import beaver.Symbol;

/**
 * JCreole-specific scanner and parser symbol.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.0
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
