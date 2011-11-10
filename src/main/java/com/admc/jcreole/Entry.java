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

/**
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class Entry implements Comparable<Entry> {
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Entry)) return false;
        return compareTo((Entry) o) == 0;
    }

    public int compareTo(Entry other) {
        if (other == null) return 1;
        if (other == this) return 0;
        if (this.label == null && other.label == null)
            return Integer.valueOf(this.entryId)
                    .compareTo(Integer.valueOf(other.entryId));
        if (this.label == null) return -1;
        if (other.label == null) return 1;
        return this.label.compareTo(other.label);
    }

    private String html;
    private String label;
    private int entryId;

    public int getEntryId() {
        return entryId;
    }

    public Entry(int entryId) {
        this.entryId = entryId;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String toString() {
        return getLabel();
    }

    /**
     * @param prefix if non-null then tag ID will be written.
     */
    public String toHtml(String prefix) {
        if (label == null)
            throw new IllegalStateException("Label not set yet for entry");
        if (html == null)
            throw new IllegalStateException(
                    "Html not set yet for entry: " + label);
        if (prefix != null && entryId < 0)
            throw new IllegalStateException(
                    "Prefix set but entryId not set for entry: " + label);
        StringBuilder sb = new StringBuilder().append("<dl");
        if (prefix != null)
                sb.append(" id=\"").append(prefix).append(entryId).append('"');
        return sb.append(">\n  <dt>").append(label).append("</td>\n  <dd>")
                .append(html).append("</dd>\n</dl>\n").toString();
    }
}
