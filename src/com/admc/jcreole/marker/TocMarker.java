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

import java.util.List;
import com.admc.jcreole.SectionHeading;

/**
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class TocMarker extends BufferMarker {
    private List<SectionHeading> sectionHeadings;
    String levelInclusions;

    /**
     * @param levelInclusions  See param levelInclusions for method
     *        SectionHeading.generateToc().
     * @see SectionHeading#generateToc
     */
    public TocMarker(int id, String levelInclusions) {
        super(id);
        this.levelInclusions = levelInclusions;
    }

    public void setSectionHeadings(List<SectionHeading> sectionHeadings) {
        this.sectionHeadings = sectionHeadings;
    }

    public String toString() {
        return "TOC Marker";
    }

    public void updateBuffer() {
        if (sectionHeadings == null)
            throw new IllegalStateException(
                    "Can't generate TOC until sectionHeadings are assigned");
        super.updateBuffer();
        targetSb.insert(offset, SectionHeading.generateToc(
                sectionHeadings, levelInclusions));
    }
}
