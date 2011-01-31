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
import com.admc.jcreole.Sections;

/**
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class TocMarker extends BufferMarker {
    private Sections sectionHeadings;
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

    /**
     * Set levelInclusions only if one has not been set already (and if the
     * levelInclusions parameter of the constructor was null).
     */
    public void setDefaultLevelInclusions(String levelInclusions) {
        if (this.levelInclusions == null)
            this.levelInclusions = levelInclusions;
    }

    public void setSectionHeadings(Sections sectionHeadings) {
        this.sectionHeadings = sectionHeadings;
    }

    public String toString() {
        return "TOC Marker";
    }

    public void updateBuffer() {
        if (sectionHeadings == null)
            throw new IllegalStateException(
                    "Can't generate TOC until sectionHeadings are assigned");
        if (levelInclusions == null)
            throw new IllegalStateException(
                    "Can't generate TOC until levelInclusions is set");
        super.updateBuffer();
        targetSb.insert(offset, sectionHeadings.generateToc(levelInclusions));
    }
}
