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
}
