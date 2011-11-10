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

import java.util.Comparator;

/**
 * Sorts the toString() of elements in same order as a dictionary.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class DictionaryComparator implements Comparator<Object> {
    public static final DictionaryComparator singleton =
            new DictionaryComparator();

    public int compare(Object o1, Object o2) {
        if (o1 == null && o2 == null) return 0;
        if (o1 == null) return -1;
        if (o2 == null) return 1;

        String s1 = o1.toString();
        String s2 = o2.toString();
        int len1 = s1.length();
        int len2 = s2.length();
        String is1 = s1.toLowerCase();
        String is2 = s2.toLowerCase();
        int delta;

        for (int i = 0; i < len1 && i < len2; i++) {
            delta = is1.charAt(i) - is2.charAt(i);
            if (delta != 0) return delta;
        }
        if (len1 != len2) return (len1 < len2) ? -1 : 1;
        for (int i = 0; i < len1 && i < len2; i++) {
            delta = s2.charAt(i) - s1.charAt(i);
            if (delta != 0) return delta;
        }
        return 0;
    }
}
