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

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import com.admc.jcreole.EntryOrdering;
import com.admc.jcreole.DictionaryComparator;

/**
 * Lifecycle <ol>
 *   <li>Instantiate</li>
 *   <li>add(DeferredUrlMarker)</li>
 *   <li>add(name)  (reading them from OOB)</li>
 *   <li>sort</li>
 *   <li>updateReferences</li>
 *   <li>EXTERNALLY updateBuffer paragraphs, incl. DeferredUrlMarkers)</li>
 *   <li>set(name, Html)  (pulling them from OOB)</li>
 *   <li>updateBuffer</li>
 * </ol>
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class MasterDefListMarker extends BodyUpdaterMarker {
    public List<DeferredUrlMarker> refMarkers =
            new ArrayList<DeferredUrlMarker>();

    public void add(DeferredUrlMarker refMarker) {
        refMarkers.add(refMarker);
    }

    public void updateReferences() {
        Entry targetEntry;
        for (DeferredUrlMarker refMarker : refMarkers) {
            targetEntry = origKeyToEntry.get(refMarker.getInUrl());
            if (targetEntry == null) continue;
            refMarker.setTargetId(targetEntry.getEntryId());
        }
    }

    public MasterDefListMarker(int id, EntryOrdering inOrdering) {
        super(id, (inOrdering == null)
                ? EntryOrdering.NAME_BY_DICTIONARY : inOrdering);
    }

    public void add(String name) {
System.err.println("+ " + name);
        Entry newEntry = new Entry(1 + entries.size());
        entries.add(newEntry);
        origKeyToEntry.put(name, newEntry);
        newEntry.setLabel(name);
    }

    /**
     * Update entryHtml for the specified reference name
     */
    public void set(String name, String entryHtml) {
        Entry targetEntry = origKeyToEntry.get(name);
        if (targetEntry == null)
            throw new IllegalArgumentException(
                    "Lost MasterDefList entry for: " + name);
        targetEntry.setHtml(entryHtml);
    }

    public void updateBuffer() {
        for (Entry entry : entries) outBuffer.append(entry.toHtml("jcmdef"));
        super.updateBuffer();
    }

    /**
     * @see BodyUpdaterMarker#sort()
     */
    public void sort() {
        switch (ordering) {
          case DEF_ORDER:
            // Just preserve original entries order
            break;
          case REF_ORDER:
            Set<String> loadedNames = new HashSet<String>();
            List<Entry> origEntries = new ArrayList<Entry>(entries);
            entries.clear();
            Entry targetEntry;
            for (DeferredUrlMarker refMarker : refMarkers) {
                // This block adds entries in reference order
                targetEntry = origKeyToEntry.get(refMarker.getInUrl());
                if (targetEntry == null) continue;
                if (loadedNames.contains(refMarker.getInUrl())) continue;
                loadedNames.add(refMarker.getInUrl());
                entries.add(targetEntry);
                origEntries.remove(targetEntry);
            }
            // Remainder of this case adds non-referenced entries in original
            // definition order
            entries.addAll(origEntries);
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
