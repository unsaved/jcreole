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

import com.admc.jcreole.TagType;

/**
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class CloseMarker extends BufferMarker {
    private String tagName;
    private TagType targetType;

    /**
     * For HTML Block and Inline tags.
     */
    public CloseMarker(int id, String tagName, TagType targetType) {
        super(id);
        this.tagName = tagName;
        this.targetType =  targetType;
    }

    /**
     * For JCX SPAN tags.
     */
    public CloseMarker(int id) {
        super(id);
    }

    public String getTagName() { return tagName; }

    public TagType getTargetType() { return targetType; }

    public String toString() {
        StringBuilder sb = new StringBuilder(getIdString()).append('/')
        .append(targetType).append(':').append(getTagName());
        if (applied) return sb.toString();
        return sb.append('@').append(offset).toString();
    }
}
