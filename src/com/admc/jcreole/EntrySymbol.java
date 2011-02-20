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

import beaver.Symbol;

/**
 * A Parser Symbol for entry definition for a glossary or foot note.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.0
 */
class EntrySymbol extends WashedSymbol {
    EntryType eType;
    int entryId;
    String name;

    public EntryType getEntryType() {
        return eType;
    }

    public EntrySymbol(EntryType eType, String name) {
        this.eType = eType;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setEntryId(int entryId) {
        this.entryId = entryId;
        if (entryId < 0 || entryId > 0xFFFF)
            throw new IllegalArgumentException(
                    "Id is not between 0 and 0xFFFF inclusive: " + entryId);
    }

    public int getEntryId() {
        return entryId;
    }

    public String getIdString() {
        return String.format("%04X", entryId);
    }

    public String getIdAttr() {
        return "jc" + ((eType == EntryType.FOOTNOTE) ? "fn" : "gl") + entryId;
    }

    /**
     * Sandwiches content between binary control characters STX and ETX.
     */
    public String toString() {
        return "\u0002" + ((eType == EntryType.GLOSSARY) ? 'G' : 'F')
                + getIdString() + super.toString() + '\u0003';
    }
}
