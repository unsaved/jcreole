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
class TabSymbol extends Token {
    private String content;
    private String label;
    private int refId;

    public TabSymbol(Token sourceToken, String wholeString, int refId) {
        super(sourceToken.getId(), null,
                sourceToken.getOffset(), sourceToken.getLine(),
                sourceToken.getColumn());
        this.refId = refId;
        int pipeIndex = wholeString.indexOf('|');
        if (pipeIndex < 1)
            throw new CreoleParseException(
                    "No tab label/content/delimiter: " + wholeString,
                    sourceToken);
        if (pipeIndex >= wholeString.length() - 1)
            throw new CreoleParseException(
                    "No tab content: " + wholeString, sourceToken);
        label = wholeString.substring(0, pipeIndex);
        content = wholeString.substring(pipeIndex + 1);
    }

    public String getLabelLi() {
        return "<li><a href=\"#jcreole_tab" + refId + "\">" + label + "</a></li>";
    }

    public String getContentBlock() {
        return "<div id=\"jcreole_tab"
                + refId + "\">\n    " + content + "\n  </div>";
    }

    public String toString() {
        return getLabelLi() + '\n' + getContentBlock();
    }
}
