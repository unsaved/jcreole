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


package com.admc.util;

import java.util.Comparator;
import java.io.File;

/**
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.2.0
 */
public class FileComparator implements Comparator<File> {
    public enum SortBy { MODIFIED, NAME, SIZE }

    private SortBy cfField;
    private boolean ascending;

    public FileComparator(SortBy cfField, boolean ascending) {
        this.ascending = ascending;
        this.cfField = cfField;
    }

    public int compare(File f1, File f2) {
        int val;
        long l1, l2;
        switch (cfField) {
          case MODIFIED:
            l1 = f1.lastModified();
            l2 = f2.lastModified();
            if (l1 == l2)
                val = 0;
            else if (l1 < l2)
                val = -1;
            else
                val = 2;
            break;
          case SIZE:
            l1 = f1.isDirectory() ? 0L : f1.length();
            l2 = f2.isDirectory() ? 0L : f2.length();
            if (l1 == l2)
                val = 0;
            else if (l1 < l2)
                val = -1;
            else
                val = 2;
            break;
          case NAME:
            String n1 = f1.getName();
            String n2 = f2.getName();
            if (n1.length() > n2.length()) n1 = n1.substring(0, n2.length());
            else if (n2.length() > n1.length()) n2 = n2.substring(0, n1.length());
            val = n1.compareToIgnoreCase(n2);
            if (val < 0) {
                val = -1;
            } else if (val > 0) {
                val = 1;
            } else {
                val = -n1.compareTo(n2);  // - to make capitals sort higher
                if (val == 0) val = -f1.getName().compareTo(f2.getName());
            }
            break;
          default:
            throw new RuntimeException("Unexpected cfField value: " + cfField);
        }
        return ascending ? val : (-val);
    }

    public String toString() {
        return (ascending ? '+' : '-') + cfField.toString();
    }
}
