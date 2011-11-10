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


package com.admc.jcreole.marker;

import com.admc.jcreole.SectionHeading;

/**
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class HeadingMarker extends BlockMarker {
    private SectionHeading sectionHeading;
    private Character formatReset;

    public HeadingMarker(
            int id, String xmlId, int level, String text) {
        super(id, "h" + level, true, false);
        sectionHeading = new SectionHeading(xmlId, level, text);
    }

    public String updatedEnumerationFormats(String inFormats) {
        if (inFormats == null)
            throw new NullPointerException("inFormats may not be null");
        if (formatReset == null) return inFormats;
        int sectionLevel = sectionHeading.getLevel();
        char oldFormat = inFormats.charAt(sectionLevel - 1);
        char newFormat = formatReset.charValue();
        if (oldFormat == newFormat) return inFormats;
        StringBuilder sb = new StringBuilder(inFormats);
        sb.setCharAt(sectionLevel - 1, newFormat);
        return sb.toString();
    }

    public void setFormatReset(char formatResetChar) {
        formatReset = Character.valueOf(formatResetChar);
    }

    public Character getFormatReset() {
        return formatReset;
    }

    public SectionHeading getSectionHeading() {
        return sectionHeading;
    }

    public void updateBuffer() {
        String sequenceLabel = sectionHeading.getDottedSequenceLabel();
        targetSb.insert(offset + 5, (sequenceLabel == null)
                ? "><span class=\"jcreole_hbody\">"
          : ("><span class=\"jcreole_hbody\"><span class=\"jcsec_enum\">&sect;"
                  + sequenceLabel + "<span> "));
        super.updateBuffer();
    }
}
