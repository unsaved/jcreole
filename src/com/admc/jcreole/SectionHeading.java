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
    // Normalized heading text (without Wikitext and HMTL)
    private final String text;
    private final String xmlId;  // HTML/XML Id
    private final int level;
    private int sequence;
    private String sequenceLabel;

    public SectionHeading(String xmlId, int level, String text) {
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
}
