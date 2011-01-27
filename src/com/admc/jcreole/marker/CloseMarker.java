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
public class CloseMarker extends BufferMarker {
    private String tagName;
    Boolean blockType;  // TRUE = Block HTML; FALSE = Inline HTML; null = JCX

    /**
     * For HTML Block and Inline tags.
     */
    public CloseMarker(int id, String tagName, boolean blockType) {
        super(id);
        this.tagName = tagName;
        this.blockType = Boolean.valueOf(blockType);
    }

    /**
     * For JCX SPAN tags.
     */
    public CloseMarker(int id) {
        super(id);
    }

    public String getTagName() { return tagName; }

    public Boolean getBlockType() { return blockType; }

    public String toString() {
        StringBuilder sb = new StringBuilder(getIdString()).append('/');
        if (blockType == null) sb.append("JCX");
        else sb.append(blockType.booleanValue() ? "BLOCK" : "INLINE");
        sb.append(':').append(getTagName());
        if (applied) return sb.toString();
        return sb.append('@').append(offset).toString();
    }
}
