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
public class LinkMarker extends BufferMarker {
    private String linkText, label;

    public LinkMarker(int id, String linkText) {
        super(id);
        if (linkText.length() < 1 || linkText.charAt(0) != '#')
            throw new IllegalStateException(
                    "Local link does not begin with '#': " + linkText);
        this.linkText = linkText;
    }

    public String getLinkText() { return linkText; }
    public String getLabel() { return label; }

    public void setLabel(String label) {
        if (this.label != null)
            throw new IllegalStateException("Label has already been set");
        this.label = label;
    }

    public void wrapLabel(String prefix, String suffix) {
        if (label == null)
            throw new IllegalStateException(
                    "Can't call wrapLabel before base label has been set");
        label = prefix + label + suffix;
    }

    public void updateBuffer() {
        super.updateBuffer();
        if (label != null) targetSb.insert(offset, label);
    }
}
