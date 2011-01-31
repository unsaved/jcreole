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
    private int[] sequences = new int[] {-1, -1, -1, -1, -1, -1};
    private String enumerationFormats;

    public String getXmlId() { return xmlId; }

    /**
     * Level here is the 'h' level, which begins at 1 not 0.
     */
    public SectionHeading(String xmlId, int level, String text) {
        if (level < 1 || level > MAX_LEVEL)
            throw new IllegalArgumentException(
                    "Unsupported heading level: " + level);
        this.xmlId = xmlId;
        this.level = level;
        this.text = text;
    }

    public void setEnumerationFormats(String enumerationFormats) {
        if (enumerationFormats == null)
            throw new NullPointerException(
                    "enumerationFormats may not be null");
        this.enumerationFormats = enumerationFormats;
    }

    public void setSequences(int[] sequences) {
        System.arraycopy(sequences, 0, this.sequences, 0, sequences.length);
    }

    public String toString() {
        return String.format(
                "%s=> %d/%d %s \"%s\"", xmlId,
                level, sequences[level-1], getDottedSequenceLabel(), text);
    }

    public int getLevel() {
        return level;
    }

    public String getText() {
        return text;
    }

    public String getSequenceLabel() {
        return getSequenceLabel(level);
    }

    public String getDottedSequenceLabel() {
        String segment = getSequenceLabel();
        if (segment == null) return null;
        StringBuilder sb = new StringBuilder(segment);
        for (int i = level - 1; i >0; i--) {
            segment = getSequenceLabel(i);
            if (segment != null) sb.insert(0, '.').insert(0, segment);
        }
        return sb.toString();
    }

    public String getSequenceLabel(int aLevel) {
        if (enumerationFormats == null)
            throw new IllegalStateException("enumerationFormats is null");
        if (enumerationFormats.length() < aLevel)
            throw new IllegalStateException(
                    "enumerationFormats is of insufficient length: "
                    + enumerationFormats.length() + " vs. " + aLevel);
        char labelType = enumerationFormats.charAt(aLevel-1);
        switch (labelType) {
          case 'x':
          case '_':
            return null;
          case 'a':
            return Character.toString((char) ('a' + sequences[aLevel-1]));
          case 'A':
            return Character.toString((char) ('A' + sequences[aLevel-1]));
          case '0':
            return Integer.toString(sequences[aLevel-1]);
          case '1':
            return Integer.toString(sequences[aLevel-1] + 1);
          default:
            throw new IllegalArgumentException(
                    "Unexpected label type: " + labelType);
        }
    }

    /**
     * @param levelInclusions  char array of length 6.  Each char is just
     *        checked for 'x' to indicate to skip that level in the TOC.
     *        If the char for an index is not 'x', then a record for that
     *        heading/section will be written according to the SectionHeading
     *        record.
     */
    public static String generateToc(
            List<SectionHeading> shs, String levelInclusions) {
        if (levelInclusions == null)
            throw new NullPointerException("levelInclusions may not be null");
        if (levelInclusions.length() != 6)
            throw new IllegalArgumentException(
                    "levelInclusions has length " + levelInclusions.length()
                    + " inead of 6:  " + levelInclusions);
        if (shs == null) return null;
        if (shs.size() < 1) return "";
        // menuLevels is 0-based just like levelInclusions.
        int[] menuLevels = new int[6];
        int nextLevel = 0;
        for (int i = 0; i < levelInclusions.length(); i++)
            menuLevels[i] = (levelInclusions.charAt(i) == 'x')
                          ? -1 : nextLevel++;
        int menuLevel = -1, newMenuLevel;
        String seqLabel;
        List<String> unravelStack = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
            new StringBuilder(>\n")
        for (SectionHeading sh : shs) {
            // sh level is 1 more than array indexes
            newMenuLevel = menuLevels[sh.level - 1];
            if (newMenuLevel < 0) continue;  // Don't display this level
            if (newMenuLevel == menuLevel) {
                sb.append("</li>\n");
            } else if (newMenuLevel > menuLevel) {
                for (int i = menuLevel + 1; i <= newMenuLevel; i++) {
                    sb.append(CreoleParser.indent(i)).append((i == 0)
                            ? "<ul class=\"jcreole_toc\">\n"
                            : "<ul>\n");
                    unravelStack.add(0, (i == newMenuLevel) ? "</li>" : "");
                    // When back out, do need to close a li?
                }
            } else if (newMenuLevel < menuLevel) {
                unravelStack.remove(); // Can only get here if we wrote an li
                                       // at this level on previous iteration.
                sb.append("</li>\n");
                for (int i = menuLevel; i >= newMenuLevel + 1; i--)
                    sb.append(CreoleParser.indent(i))
                            .append(unravelStack.remove().append("</ul>");
            }
            menuLevel = newMenuLevel;
            sb.append(CreoleParser.indent(menuLevel+1))
                    .append("<li><a href=\"#").append(sh.xmlId).append("\">");
            seqLabel = sh.getSequenceLabel();
            if (seqLabel != null) {
                sb.append("<span class=\"jcx_seqLabel\">")
                .append(seqLabel).append("</span> ");
            }
            sb.append(sh.text).append("</a>\n");
        }
        unravelStack.remove(); // Can only get here if we wrote an li
                               // at this level on previous iteration.
        for (int i = menuLevel; i >= 1; i--)
            sb.append(CreoleParser.indent(i)).append("</ul></li>\n");
        return sb.append("</ul>").toString();
    }
}
