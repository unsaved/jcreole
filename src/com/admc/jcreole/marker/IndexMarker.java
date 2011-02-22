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
import java.util.Collections;
import com.admc.jcreole.EntryOrdering;
import com.admc.jcreole.CreoleParseException;
import com.admc.jcreole.DictionaryComparator;

/**
 * Lifecycle <ol>
 *   <li>Instantiate</li>
 *   <li>add(IndexEntryMarker)</li>
 *   <li>generateEntries()</li>
 *   <li>sort</li>
 *   <li>EXTERNALLY updateBuffer IndexEntryMarkers)</li>
 *   <li>updateBuffer</li>
 * </ol>
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class IndexMarker extends BodyUpdaterMarker {
    public List<IndexEntryMarker> refMarkers =
            new ArrayList<IndexEntryMarker>();
    public Map<Entry, List<IndexEntryMarker>> entryLinks =
            new HashMap<Entry, List<IndexEntryMarker>>();

    public void add(IndexEntryMarker refMarker) {
        refMarkers.add(refMarker);
    }

    public IndexMarker(int id, EntryOrdering inOrdering) {
        super(id, (inOrdering == null)
                ? EntryOrdering.NAME_BY_DICTIONARY : inOrdering);
        if (ordering == EntryOrdering.DEF_ORDER)
            throw new CreoleParseException(
                    "Ordering not supported for FootNotes: " + ordering);
    }

    public void generateEntries() {
        Entry entry;
        int counter = 0;
        for (IndexEntryMarker refMarker : refMarkers) {
            if (origKeyToEntry.containsKey(refMarker.getName())) {
                entry = origKeyToEntry.get(refMarker.getName());
            } else {
                entry = new Entry(1 + entries.size());
                entry.setLabel(refMarker.getName());
                entryLinks.put(entry, new ArrayList<IndexEntryMarker>());
                entries.add(entry);
                origKeyToEntry.put(refMarker.getName(), entry);
            }
            entryLinks.get(entry).add(refMarker);
            refMarker.setTargNum(++counter);
        }
        StringBuilder sb = new StringBuilder();
        int refCount;
        for (Map.Entry<Entry, List<IndexEntryMarker>> e :
                entryLinks.entrySet()) {
            sb.setLength(0);
            refCount = 0;
            for (IndexEntryMarker refMarker : e.getValue()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append("<a href=\"#jcindexed").append(refMarker.getTargNum())
                        .append("\">").append(++refCount).append("</a>");
            }
            e.getKey().setHtml(sb.toString());
        }
    }

    public void updateBuffer() {
        for (Entry entry : entries) outBuffer.append(entry.toHtml(null));
        super.updateBuffer();
    }

    /**
     * @see BodyUpdaterMarker#sort()
     */
    public void sort() {
        switch (ordering) {
          case REF_ORDER:
            break;
          case NAME_BY_DICTIONARY:
            Collections.sort(entries, DictionaryComparator.singleton);
            break;
          case NAME_BY_JAVA:
            Collections.sort(entries);
            break;
        }
    }
}
