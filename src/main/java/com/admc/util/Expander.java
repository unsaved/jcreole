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
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;
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
 * References look like ${this} or ${-this} or ${!this}.
 * If the map element ('this' in these examples) is set, these references will
 * all expand to the same exact thing (obviously the value of the map element
 * with key 'this').
 * The difference only applies if there is no map element with key 'this'.
 * In that case, ${this} remains exactly as it was (the input Creole is not
 * changed at all); ${-this} is removed (i.e. replace with an empty string);
 * and ${!this} will cause the expand() method to throw.
 * <p> </p>
 * I would much prefer to write this with Groovy, but GroovyDoc sucks and I
 * want to provide a real API Spec for integrators to work from.
 * <p> </p>
 * Nested definitions, like a map value containing '${ref}', are supported, but
 * the nested values are de-referenced by the put* methods, not in the
 * expand method.  Therefore, they are dereferenced at setup time, not at
 * expand time.
 * Consequently, if you use putAll() call with intra-references, you must
 * ensure that you use an order-preserving Map implementation, and that the
 * referents' values are completely defined before the referers.
 * Two other ways to satisfy this use-case are to use your own loop and call
 * put*() instead of putAll(); or to move referred-to definitions into an
 * additional map that is fed to putAll() before your main map.
 * <p> </p>
 * The escape mechanism is \${, \$[, \$(.  All that will happen is the
 * backslash there will be removed.
 * </p>
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1.2
 */
public class Expander {
    public enum PairedDelims {
        /** NO IDEA WHY THIS DOES NOT WORK!!
        CURLY('{', '}'), RECTANGULAR('[', ']'), ROUNDED('(', ')');
        char lChar, rChar;
        PairedDelims(char l, char r) {
            lChar = l;
            rChar = r;
        }
        final public Pattern refPattern = Pattern.compile(
                "\\$\\" + lChar + "([-!])?([^" + rChar + '\\' + lChar
                + "]+)\\" + rChar);
        */
        CURLY('{', "\\$\\{([-!])?([^}\\{]+)\\}"),
        RECTANGULAR('[', "\\$\\[([-!])?([^]\\[]+)\\]"),
        ROUNDED('(', "\\$\\(([-!])?([^)\\(]+)\\)");

        final public Pattern refPattern;
        final String escapeFrom, escapeTo;
        PairedDelims(char lChar, String s) {
            refPattern = Pattern.compile(s);
            escapeFrom = "\\$" + lChar;
            escapeTo = "$" + lChar;
        }
        public CharSequence preserveEscapes(CharSequence s) {
            if (s instanceof String) {
                if (((String) s).indexOf(escapeFrom) < 0) return s;
                return ((String) s).replace(escapeFrom, "\u0002");
            }
            if (!(s instanceof StringBuilder))
                throw new RuntimeException("CharSequence.preserveEscapes only "
                        + "works with Strings and StringBuilders");
            StringBuilder sb = (StringBuilder) s;
            // N.b. we will modify the given StringBuilder and return it.
            // If this causes any problem, then copy it here.
            int lastI;
            while (true) {
                lastI = sb.lastIndexOf(escapeFrom);
                if (lastI < -0) return sb;
                sb.replace(lastI, lastI + 3, "\u0002");
            }
        }
        public void escape(StringBuilder sb) {
            int lastI;
            while (true) {
                lastI = sb.lastIndexOf("\u0002");
                if (lastI < -0) return;
                sb.replace(lastI, lastI + 1, escapeTo);
            }
        }
    }

    public Expander(PairedDelims pd) {
        pairedDelims = pd;
    }

    private PairedDelims pairedDelims;
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
     * Wrapper for put(String, String, String, boolean),
     * with no (null) namespace and value expansion.
     *
     * @see #put(String, String, String, boolean)
     */
    public String put(String newKey, String newVal) {
        return put(null, newKey, newVal, true);
    }

    /**
     * Wrapper for put(String, String, String, boolean),
     * with no (null) namespace.
     *
     * @see #put(String, String, String, boolean)
     */
    public String put(String newKey, String newVal, boolean expandVal) {
        return put(null, newKey, newVal, expandVal);
    }

    /**
     * N.b. the return value of this method differs drastically from that of
     * java.util.Map.put(String, String).
     * <p>
     * See Class level Javadoc for details about nested values.
     * </p>
     *
     * @param ns  Namespace prefixed (with prefixDelimiter) to key.
     * @return Actual key name added, without prefix (if any), if key changed.
     */
    public String put(
            String ns, String newKey, String newVal, boolean expandVal) {
        if (ns != null && anyIllegalCharPattern.matcher(ns).matches())
            throw new IllegalArgumentException(
                    "Specified namespache contains illegal character(s): "
                    + ns);
        String prefix = (ns == null) ? "" : (ns + prefixDelimiter);
        String retVal = null;
        String key;
        if (anyIllegalCharPattern.matcher(newKey).matches()) {
            key = illegalCharPattern.matcher(newKey).replaceAll("_");
            retVal = key;
        } else {
            key = newKey;
        }
        map.put(prefix + key, expandVal ? expand(newVal).toString() : newVal);
        return retVal;
    }

    /**
     * Wrapper for putAll(String, Map, boolean),
     * with no (null) namespace and value expansion.
     *
     * @see #putAll(String, Map, boolean)
     */
    public Map<String, String> putAll(Map<String, String> inMap) {
        return putAll(null, inMap, true);
    }

    /**
     * Wrapper for putAll(String, Map, boolean), with no (null) namespace.
     *
     * @see #putAll(String, Map, boolean)
     */
    public Map<String, String> putAll(
            Map<String, String> inMap, boolean expandVals) {
        return putAll(null, inMap, expandVals);
    }

    /**
     * Exact same behavior as putAll(String, Map, boolean)
     *
     * @see #putAll(String, Map, boolean)
     */
    public Map<String, String> putAll(
            String ns, Properties ps, boolean expandVals) {
        String changedKey;
        Map<String, String> renameMap = new HashMap<String, String>();
        for (Map.Entry entry : ps.entrySet()) {
            changedKey = put(ns, entry.getKey().toString(),
                    entry.getValue().toString(), expandVals);
            if (changedKey != null)
                renameMap.put(entry.getKey().toString(), changedKey);
        }
        return renameMap;
    }

    /**
     * If the specified ns is non-null, then it is used with the current
     * ns for this Expander instance.
     * <p>
     * See Class level Javadoc for details about nested values.
     * </p>
     *
     * @param ns  Namespace prefixed (with prefixDelimiter) to key.
     * @return Map of keys that were renamed.
     */
    public Map<String, String> putAll(
            String ns, Map<String, String> inMap, boolean expandVals) {
        String changedKey;
        Map<String, String> renameMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : inMap.entrySet()) {
            changedKey = put(ns, entry.getKey(), entry.getValue(), expandVals);
            if (changedKey != null) renameMap.put(entry.getKey(), changedKey);
        }
        return renameMap;
    }

    /**
     * Same exact contract as expand(CharSequence), but returns a String.
     *
     * @see #expand(CharSequence)
     */
    public String expandToString(CharSequence inString) {
        return expand(inString).toString();
    }

    /**
     * @throws IllegalArgumentException if inString contains an unsatisfied
     *         ! reference (like ${!ref}).
     */
    synchronized public StringBuilder expand(CharSequence inString) {
        Set throwRefs = new HashSet<String>();
        CharSequence seq = pairedDelims.preserveEscapes(inString);
        Matcher matcher = pairedDelims.refPattern.matcher(seq);
        int prevEnd = 0;
        String val;
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            if (throwRefs.size() < 1)
                sb.append(seq.subSequence(prevEnd, matcher.start()));
            prevEnd = matcher.end();
            if (map.containsKey(matcher.group(2))) {
                if (throwRefs.size() < 1) sb.append(map.get(matcher.group(2)));
                continue;
            }
            if (matcher.group(1) == null) {
                if (throwRefs.size() < 1) sb.append(matcher.group(0));
                continue;
            }
            switch (matcher.group(1).charAt(0)) {
              case '!':
                throwRefs.add(matcher.group(2));
                break;
              case '-':
                break;
              default:
                throw new RuntimeException(
                      "Unexpected behavior pref string: " + matcher.group(1));
            }
        }
        if (throwRefs.size() > 0)
            throw new IllegalArgumentException(
                    "Unsatisfied ! reference(s): " + throwRefs);
        sb.append(seq.subSequence(prevEnd, seq.length()));
        pairedDelims.escape(sb);
        return sb;
    }

    static public void main(String[] sa) {
        System.out.println(new Expander(PairedDelims.CURLY).expand(sa[0]));
    }

    public String toString() { return map.toString(); }
}
