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
class DtSymbol extends Token {
    private String content;

    public DtSymbol(Token sourceToken) {
        super(sourceToken.getId(), null,
                sourceToken.getOffset(), sourceToken.getLine(),
                sourceToken.getColumn());
    }

    public void setContent(String content) {
        // Get rid of extra newline:
        this.content = (content.length() > 0
                && content.charAt(content.length()-1) == '\n')
                ? content.substring(content.length()-1) : content;
        if (this.content.length() < 1)
            throw new IllegalArgumentException(
                    "Empty def list items prohibited");
    }

    public String getContent() {
        // TODO:  Consider whether useful or counter-productive to make the
        // dt and dd jcxBlock-addressable by writing markers here.
        // Are these more like block or more like inline?
        int colonOffset = content.indexOf(':');
        StringBuilder sb = new StringBuilder("<dt>");
        if (colonOffset > -1) {
            // Get rid of extra newline:
            int postDt = (colonOffset > 0
                    && content.charAt(colonOffset - 1) == '\n')
                ? (colonOffset - 1) : colonOffset;
            sb.append(content.substring(0, postDt)).append("</dt>");
            if (colonOffset < content.length() - 1)
                sb.append("\n  <dd>").append(content.substring(colonOffset + 1))
                .append("</dd>");
        } else {
            sb.append(content).append("</dt>");
        }
        return sb.toString();
    }

    public String toString() {
        return "<dl>" + getContent() + "</dl>";
    }
}
