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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parser Token especially for List Items.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.0
 */
class LiSymbol extends Token {
    private char type;
    private String content;
    private boolean headed;
    private char enumSymbol;

    static Pattern EnumSymbolPattern = Pattern.compile("(?s)([1aAiI]#)(.*)");

    public LiSymbol(Token sourceToken) {
        super(sourceToken.getId(), null,
                sourceToken.getOffset(), sourceToken.getLine(),
                sourceToken.getColumn(),
                (sourceToken.getIntParam() < 0)
                ? (-sourceToken.getIntParam())
                : sourceToken.getIntParam());
        headed = sourceToken.getIntParam() < 0;
        String s = sourceToken.getStringVal();
        if (s == null) throw new NullPointerException();
        if (s.length() != 1)
            throw new IllegalArgumentException("Malformatted type: " + s);
        type = s.charAt(0);
        if (getIntParam() < 1 || getIntParam() > 6)
            throw new IllegalArgumentException(
                    "Illegal list level: " + getIntParam());
    }

    public void setContent(final String content) {
        Matcher matcher = EnumSymbolPattern.matcher(content);
        if (matcher.matches()) {
            enumSymbol = matcher.group(1).charAt(0);
            this.content = matcher.group(2);
        } else {
            this.content = content;
        }
        if (this.content.length() < 1)
            throw new IllegalArgumentException("Empty list items prohibited");
    }

    public char getEnumSymbol() {
        return enumSymbol;
    }

    public char getType() {
        return type;
    }

    public int getLevel() {
        return getIntParam();
    }

    public String getContent() {
        if (!headed) return content;
        // TODO:  Consider whether useful or counter-productive to make the
        // ih and id spans jcxSpan-addressable by writing markers here.
        int pipeOffset = content.indexOf('|');
        StringBuilder sb = new StringBuilder();
        Matcher matcher = EnumSymbolPattern.matcher(content);
        if (matcher.matches()) sb.append(matcher.group(1));
        if (pipeOffset != 0)
            sb.append("<span class=\"jcreole_lh\">")
            .append(content.substring(matcher.matches() ? 2 : 0,
                    (pipeOffset > 0) ? pipeOffset : content.length()))
            .append("</span>");
        if (pipeOffset > -1 && pipeOffset != content.length() - 1)
            sb.append("<span class=\"jcreole_ld\">")
                .append(content.substring(pipeOffset + 1))
                .append("</span>");
        return sb.toString();
    }

    public String toString() {
        return Character.toString(type) + getLevel()
                + ": <li level=\"" + getLevel() + "\">" + content + '>';
    }
}
