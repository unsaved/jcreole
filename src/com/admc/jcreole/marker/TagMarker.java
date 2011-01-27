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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * Represents a HTML Tag, JCX-created or not.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
abstract public class TagMarker extends BufferMarker {
    private boolean writeAttr;
    private List<String> cssClasses = new ArrayList<String>();
    private String tagName;
    private boolean atomic;

    /**
     * @writeAttr  whether to write the 'class' attribute name (and = and "),
     *             as opposed to just inserting names of classes.
     */
    public TagMarker(
            int id, String tagName, boolean writeAttr, boolean atomic) {
        super(id);
        this.tagName = tagName;
        this.writeAttr = writeAttr;
        this.atomic = atomic;
    }

    public String getTagName() {
        return tagName;
    }

    public void add(String className) {
        if (!cssClasses.contains(className)) cssClasses.add(className);
    }

    /**
     * Removes the single character at the indicated point, then, if there
     * are any class names to be written, inserts the list at that point.
     */
    public void updateBuffer() {
        super.updateBuffer();
        if (cssClasses.size() < 1) return;
        String classesString = StringUtils.join(cssClasses, ' ');
        targetSb.insert(offset, writeAttr
                ? (" class=\"" + classesString + '\"')
                : (" " + classesString));
    }

    public String toString() {
        return getIdString() + '/'
                + getClass().getName().replaceFirst(".*\\.", "")
                + ':' + getTagName() + (applied ? "" : ("@" + offset));
    }

    public boolean isAtomic() { return atomic; }
}
