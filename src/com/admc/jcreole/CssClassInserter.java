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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * Inserts CSS class names into the parser output.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class CssClassInserter implements Comparable<CssClassInserter> {
    /* Undecided about whether to store source location offset/line/col.
     * Would have to complicate parser code by passiing around the
     * originating non-terminals all over the place. */
    private boolean blockType, jcxType, writeAttr;
    protected List<String> cssClasses = new ArrayList<String>();
    private int insertionOffset = -1;
    int id = -1;
    protected StringBuilder targetSb;

    public String toString() {
        return getIdString() + '@' + insertionOffset + ": "
            + (blockType ? 'T' : 'F')
            + (jcxType ? 'T' : 'F')
            + (writeAttr ? 'T' : 'F')
            + "  Classes: " + cssClasses;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof CssClassInserter)) return false;
        return compareTo((CssClassInserter) o) == 0;
    }

    public int compareTo(CssClassInserter other) {
        return Integer.valueOf(insertionOffset).compareTo(
                Integer.valueOf(other.insertionOffset));
    }

    /**
     * @param blockType False for pure inline HTML attrs, true for all others
     *                  (including semi-blocks like TD).
     * @param jcxType  True if a jcx_ element.
     * @param writeAttr  If need to write the ' class="' + '"' to surround the
     *                   output class names (otherwise just output the class
     *                   names preceded with one space).
     *                   Set this true unless your beginning tag already has
     *                   a 'class' attr.
     */
    public CssClassInserter(int id,
            boolean blockType, boolean jcxType, boolean writeAttr) {
        this.id = id;
        if (id < 0 || id > 0xFFFF)
            throw new IllegalArgumentException(
                    "Id is not between 0 and 0xFFFF inclusive: " + id);
        this.blockType = blockType;
        this.jcxType = jcxType;
        this.writeAttr = writeAttr;
    }

    public String getIdString() {
        return String.format("%04X", id);
    }

    public int getInsertionOffset() {
        return insertionOffset;
    }

    public void add(String className) {
        if (!cssClasses.contains(className)) cssClasses.add(className);
    }

    /**
     * Confirms that a marker for this inserter exists at the indicated point
     * in the supplied StringBuilder.
     *
     * @throws IllegalStateException if marker for this CssClassInsert not
     *         found at the indicated location.
     */
    public void validate() {
        if (targetSb == null || insertionOffset < 1)
            throw new IllegalStateException(
                    "targetSb or insertionOffset not initialized");
        if (targetSb.length() < insertionOffset + 4)
            throw new IllegalStateException(
                    "StringBuilder not long enough to contain marker at "
                    + insertionOffset);
        if (targetSb.charAt(insertionOffset) != '\u001a')
            throw new IllegalStateException(
                    "Missing binary SUB char at offset " + insertionOffset);
        if (!targetSb.substring(insertionOffset + 1, insertionOffset + 5)
                .equals(getIdString()))
            throw new IllegalStateException(
                    "Marker ID mismatch.  Expected " + getIdString()
                    + " but is '" + targetSb.substring(
                    insertionOffset + 1, insertionOffset + 5) + "'");
    }

    public void setContext(StringBuilder targetSb, int insertionOffset) {
        this.targetSb = targetSb;
        this.insertionOffset = insertionOffset;
        validate();
    }

    /**
     * Removes the single character at the indicated point, then, if there
     * are any class names to be written, inserts the list at that point.
     */
    public void insert() {
        validate();  // Rechecking since contents of the targetSb could easily
                     // have been shifted since setContext was called.
        targetSb.delete(insertionOffset, insertionOffset + 5);
        if (cssClasses.size() < 1) return;
        String classesString = StringUtils.join(cssClasses, ' ');
        targetSb.insert(insertionOffset, writeAttr
                ? (" class=\"" + classesString + '\"')
                : (" " + classesString));
    }
}
