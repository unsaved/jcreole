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
import java.util.Set;
import java.util.HashSet;
import com.admc.jcreole.EntryOrdering;
import com.admc.jcreole.CreoleParseException;

/**
 * Lifecycle <ol>
 *   <li>Instantiate</li>
 *   <li>add(FootNoteRefMarker)</li>
 *   <li>add(name)  (reading them from OOB)</li>
 *   <li>sort</li>
 *   <li>updateReferences</li>
 *   <li>EXTERNALLY updateBuffer paragraphs, incl. FootNoteRefMarkers)</li>
 *   <li>set(name, Html)  (pulling them from OOB)</li>
 *   <li>updateBuffer</li>
 * </ol>
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class FootNotesMarker extends BodyUpdaterMarker {
    public List<FootNoteRefMarker> refMarkers =
            new ArrayList<FootNoteRefMarker>();

    public void add(FootNoteRefMarker refMarker) {
        refMarkers.add(refMarker);
    }

    public void updateReferences() {
        Entry targetEntry;
        for (FootNoteRefMarker refMarker : refMarkers) {
            targetEntry = origKeyToEntry.get(refMarker.getName());
            if (targetEntry == null)
                throw new IllegalStateException("Orphaned footNote ref");
            refMarker.setEntryLabel(targetEntry.getLabel());
            refMarker.setTargNum(targetEntry.getEntryId());
        }
    }

    public FootNotesMarker(int id, EntryOrdering inOrdering) {
        super(id, (inOrdering == null) ? EntryOrdering.REF_ORDER : inOrdering);
        switch (ordering) {
          case NAME_BY_JAVA:
          case NAME_BY_DICTIONARY:
            throw new CreoleParseException(
                    "Ordering not supported for FootNotes: " + ordering);
        }
    }

    public void add(String name) {
        if (origKeyToEntry.containsKey(name))
            throw new CreoleParseException(
                    "Duplicate footnotes definition for: " + name);
        Entry newEntry = new Entry(1 + entries.size());
        entries.add(newEntry);
        origKeyToEntry.put(name, newEntry);
    }

    /**
     * Update entryHtml for the specified reference name
     */
    public void set(String name, String entryHtml) {
        origKeyToEntry.get(name).setHtml(entryHtml);
    }

    public void updateBuffer() {
        for (Entry entry : entries) outBuffer.append(entry.toHtml("jcfn"));
        super.updateBuffer();
    }

    /**
     * @see BodyUpdaterMarker#sort()
     */
    public void sort() {
        switch (ordering) {
          case DEF_ORDER:
            // Just preserve original entries order
            for (Entry entry : entries)
                entry.setLabel(Integer.toString(entry.getEntryId()));
            break;
          case REF_ORDER:
            Set<String> loadedNames = new HashSet<String>();
            List<Entry> origEntries = new ArrayList<Entry>(entries);
            entries.clear();
            Entry targetEntry;
            for (FootNoteRefMarker refMarker : refMarkers) {
                // This block adds entries in reference order
                targetEntry = origKeyToEntry.get(refMarker.getName());
                if (targetEntry == null)
                    throw new CreoleParseException(
                            "No entry defined for referenced name: "
                            + refMarker.getName());
                if (loadedNames.contains(refMarker.getName())) continue;
                loadedNames.add(refMarker.getName());
                entries.add(targetEntry);
                origEntries.remove(targetEntry);
                targetEntry.setLabel(Integer.toString(entries.size()));
            }
            // Remainder of this case adds non-referenced entries in original
            // definition order
            for (Entry entry : origEntries) entry.setLabel("unreferenced");
            entries.addAll(origEntries);
            break;
        }
    }
}
