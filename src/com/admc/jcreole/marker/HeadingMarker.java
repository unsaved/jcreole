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

    public HeadingMarker(
            int id, String xmlId, int level, String text) {
        super(id, "h" + level, true, false);
        sectionHeading = new SectionHeading(xmlId, level, text);
    }

    public SectionHeading getSectionHeading() {
        return sectionHeading;
    }
}
