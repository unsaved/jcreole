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

import java.util.List;

/**
 * Encapsulates details about a HTML Heading (h1, h2, etc.) that allow it to be
 * used effectively as a section delimiter.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class SectionHeading {
    public final static int MAX_LEVEL = 6;

    // Normalized heading text (without Wikitext and HMTL)
    private final String text;
    private final String xmlId;  // HTML/XML Id
    private final int level;
    private int sequence;
    private String sequenceLabel;

    public String getXmlId() { return xmlId; }

    public SectionHeading(String xmlId, int level, String text) {
        if (level < 1 || level > MAX_LEVEL)
            throw new IllegalArgumentException(
                    "Unsupported heading level: " + level);
        this.xmlId = xmlId;
        this.level = level;
        this.text = text;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public void setSequenceLabel(String SequenceLabel) {
        this.sequenceLabel = sequenceLabel;
    }

    public String toString() {
        return String.format("%s=> %d/%d %s \"%s\"",
                xmlId, level, sequence, sequenceLabel, text);
    }

    public int getLevel() {
        return level;
    }

    public String getText() {
        return text;
    }

    /**
     * @param levelsToDisplay is an array that must contain non-null String
     *        (incl. "") for each level to display.
     *        N.b. this array is 0-based, with index 0 corresponding to h1.
     */
    public static String generateToc(
            List<SectionHeading> shs, String[] levelsToDisplay) {
        if (shs == null) return null;
        if (shs.size() < 1) return "";
        // menuLevels is 0-based just like levelsToDisplay.
        int[] menuLevels = new int[6];
        int nextLevel = 0;
        for (int i = 0; i < levelsToDisplay.length; i++)
            menuLevels[i] = (levelsToDisplay[i] == null) ? -1 : nextLevel++;
        int menuLevel = 0, newMenuLevel;
        String seqLabel;
        StringBuilder sb = new StringBuilder("<ol class=\"jcreole_toc\">\n");
        for (SectionHeading sh : shs) {
            // sh level is 1 more than array indexes
            newMenuLevel = menuLevels[sh.level - 1];
            if (newMenuLevel < 0) continue;  // Don't display this level
            if (newMenuLevel > menuLevel) {
                for (int i = menuLevel + 1; i <= newMenuLevel; i++)
                    sb.append(CreoleParser.indent(i)).append("<li><ol>\n");
            } else if (newMenuLevel < menuLevel) {
                for (int i = menuLevel; i >= newMenuLevel + 1; i--)
                    sb.append(CreoleParser.indent(i)).append("</ol></li>\n");
            }
            menuLevel = newMenuLevel;
            sb.append(CreoleParser.indent(menuLevel+1))
                    .append("<li><a href=#\"").append(sh.xmlId).append("\">");
            seqLabel = sh.sequenceLabel;
            if (seqLabel != null) {
                sb.append("<span class=\"jcx_seqLabel\">")
                .append(seqLabel).append("</span>");
            }
            sb.append(sh.text).append("</li>\n");
        }
        for (int i = menuLevel; i >= 1; i--)
            sb.append(CreoleParser.indent(i)).append("</ol></li>\n");
        return sb.append("</ol>").toString();
    }
}
