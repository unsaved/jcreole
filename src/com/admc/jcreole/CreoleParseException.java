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
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.0
 */
public class CreoleParseException extends RuntimeException {
    static final long serialVersionUID = 8504782268721814533L;

    private int offset = -1, line = -1, column = -1;

    /* I keep vacillating about whether to store 0-based offsets of 1-based
     * offsets.  Sticking with the scanner-side convention for now.
     */
    public int getOffset() { return offset; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    /**
     * Would like to eliminate this so all of these exceptions will give a
     * good indication of point-of-failure.
     * Unfortunately, it looks like at least when post-processing, it would
     * add too much complexity to the parser to report on source locations.
     */
    public CreoleParseException(String msg) {
        super(msg);
    }

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
