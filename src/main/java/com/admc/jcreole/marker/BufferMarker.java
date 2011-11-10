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

import org.apache.commons.lang.StringUtils;

/**
 * Inserts CSS class names into the parser output.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
abstract public class BufferMarker implements Comparable<BufferMarker> {
    /* Undecided about whether to store source location offset/line/col.
     * Would have to complicate parser code by passiing around the
     * originating non-terminals all over the place. */
    protected int offset = -1;
    protected int id = -1;
    protected StringBuilder targetSb;
    public static final char markerChar = '\u001a';
    protected boolean applied;

    public String toString() { return getIdString() + '@' + offset; }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof BufferMarker)) return false;
        return compareTo((BufferMarker) o) == 0;
    }

    public int compareTo(BufferMarker other) {
        if (this == other) return 0;
        if (offset < 0 && other.offset < 0)
            return Integer.valueOf(id).compareTo(Integer.valueOf(other.id));
        return Integer.valueOf(offset).compareTo(Integer.valueOf(other.offset));
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
    protected BufferMarker(int id) {
        this.id = id;
        if (id < 0 || id > 0xFFFF)
            throw new IllegalArgumentException(
                    "Id is not between 0 and 0xFFFF inclusive: " + id);
    }

    public String getIdString() {
        return String.format("%04X", id);
    }

    public String getMarkerString() {
        return Character.toString(markerChar) + getIdString();
    }

    public int getOffset() {
        return offset;
    }

    /**
     * Confirms that a marker for this inserter exists at the indicated point
     * in the supplied StringBuilder.
     *
     * @throws IllegalStateException if marker for this CssClassInsert not
     *         found at the indicated location.
     */
    public void validate() {
        if (targetSb == null || offset < 1)
            throw new IllegalStateException(
                    "targetSb or offset not initialized");
        if (targetSb.length() < offset + 4)
            throw new IllegalStateException(
                    "StringBuilder not long enough to contain marker at "
                    + offset);
        if (targetSb.charAt(offset) != '\u001a')
            throw new IllegalStateException(
                    "Missing binary SUB char at offset " + offset);
        if (!targetSb.substring(offset + 1, offset + 5)
                .equals(getIdString()))
            throw new IllegalStateException(
                    "Marker ID mismatch.  Expected " + getIdString()
                    + " but is '" + targetSb.substring(
                    offset + 1, offset + 5) + "'");
    }

    public void setContext(StringBuilder targetSb, int offset) {
        this.targetSb = targetSb;
        this.offset = offset;
        validate();
    }

    /**
     * Removes the single character at the indicated point.
     * Some subclasses will need to do more than this to update the buffer.
     */
    public void updateBuffer() {
        if (applied)
            throw new IllegalStateException(
                    "This marker already applied: " + this);
        validate();  // Rechecking since contents of the targetSb could easily
                     // have been shifted since setContext was called.
        targetSb.delete(offset, offset + 5);
    }
}
