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
 * Parser Token especially for List Items.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.0
 */
class LiSymbol extends Token {
    private char type;
    private String content;

    public LiSymbol(Token sourceToken) {
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
