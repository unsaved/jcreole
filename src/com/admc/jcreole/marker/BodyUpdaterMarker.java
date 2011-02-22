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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.admc.jcreole.EntryOrdering;

/**
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
abstract public class BodyUpdaterMarker extends BufferMarker {
    protected Map<String, Entry> origKeyToEntry = new HashMap<String, Entry>();
    protected List<Entry> entries = new ArrayList<Entry>();
    protected StringBuilder outBuffer = new StringBuilder();
    protected EntryOrdering ordering;

    public BodyUpdaterMarker(int id, EntryOrdering ordering) {
        super(id);
        this.ordering = ordering;
    }

    public void updateBuffer() {
        super.updateBuffer();
        targetSb.insert(offset, outBuffer.toString());
    }

    abstract public void sort();
}
