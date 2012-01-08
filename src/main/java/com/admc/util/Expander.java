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

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Expands ${references} in Strings.
 * <p>
 * Map keys are constrained to the character set [\w.] by converting characters
 * outside of that range to underscores.
 * The putAll methods return a map describing keys that have been renamed
 * according to this algorithm.
 * <p> </p>
 * I would much prefer to write this with Groovy, but GroovyDoc sucks and I
 * want to provide a real API Spec for integrators to work from.
 * </p>
 */
public class Expander {
    private static Pattern
            anyIllegalCharPattern = Pattern.compile(".*[^.\\w].*");
    private static Pattern illegalCharPattern = Pattern.compile("[^.\\w]");
    private Map<String, String> map = new HashMap<String, String>();
    private char prefixDelimiter = '|';

    public void setPrefixDelimiter(char newDelimiter) {
        prefixDelimiter = newDelimiter;
    }

    /**
     * Returns a copy of the current state of the internal map.
     */
    private Map<String, String> getMap() {
        return new HashMap<String, String>(map);
    }

    /**
     * Wrapper for putAll(String, Map), with no (null) prefix.
     */
    public Map<String, String> putAll(Map<String, String> inMap) {
        return putAll(null, inMap);
    }

    /**
     * If the specified ns is non-null, then it is used with the current
     * ns for this Expander instance.
     *
     * @param ns  Namespace prefixed (with prefixDelimiter) to key.
     * @return Map of keys that were renamed.
     */
    public Map<String, String> putAll(String ns, Map<String, String> inMap) {
        if (ns != null && anyIllegalCharPattern.matcher(ns).matches())
            throw new IllegalArgumentException(
                    "Specified namespache contains illegal character(s): "
                    + ns);
        String prefix = (ns == null) ? "" : (ns + prefixDelimiter);
        String key;
        Map<String, String> renameMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : inMap.entrySet()) {
            if (anyIllegalCharPattern.matcher(entry.getKey()).matches()) {
                key = illegalCharPattern.matcher(
                        entry.getKey()).replaceAll("_");
                renameMap.put(entry.getKey(), key);
            } else {
                key = entry.getKey();
            }
            map.put(prefix + key, entry.getValue());
        }
        return renameMap;
    }

    public String expand(String inString) {
        return inString;
    }
}
