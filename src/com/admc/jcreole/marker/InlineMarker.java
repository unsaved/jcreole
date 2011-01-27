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
public class InlineMarker extends TagMarker {
    private boolean atomic;

    public InlineMarker(
            int id, String tagName, boolean writeAttr, boolean atomc) {
        super(id, tagName, writeAttr);
        this.atomic = atomic;
    }

    public boolean isAtomic() { return atomic; }
}
