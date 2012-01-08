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

import static org.junit.Assert.*;
import java.util.Map;
import java.util.HashMap;

/**
 * A JUnit unit test.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 */
public class ExpanderTest {
    private Expander expander = new Expander();

    private static Map<String, String> toMap(String... sa) {
        Map<String, String> newMap = new HashMap<String, String>();
        if (sa.length != (sa.length / 2) * 2)
            throw new IllegalArgumentException(
                    "Param sa doesn't have even number of params: "
                    + sa.length);
        for (int i = 0; i < sa.length; i += 2) newMap.put(sa[i], sa[i+1]);
        return newMap;
    }

    @org.junit.Test
    public void noExpands() {
        expander.putAll(toMap("alpha", "one", "beta", "two"));
        assertEquals("one, two\nthree, four",
                expander.expandToString("one, two\nthree, four"));
    }

    @org.junit.Test
    public void noMatches() {
        expander.putAll(toMap("alpha", "one", "beta", "two"));
        assertEquals("one, ${unset1}, two\nthree, , four",
            expander.expandToString("one, ${unset1}, two\nthree, ${-unset2}, four"));
    }

    @org.junit.Test(expected=IllegalArgumentException.class)
    public void illegalNs() {
        expander.putAll("abc-def", toMap("alpha", "one", "beta", "two"));
    }

    @org.junit.Test
    public void keyRenames() {
        assertEquals(toMap("al-pha", "al_pha", " gamma", "_gamma"),
                expander.putAll(toMap(
                "al-pha", "one", "beta", "t o", " gamma", "thr-e")));
    }

    @org.junit.Test
    public void nestedDevs() {
        expander.putAll(toMap("alpha", "one", "beta", "two"));
        expander.putAll(toMap("gamma", "three", "delta", "pre${alpha}post"));
        assertEquals("one two three preonepost",
                expander.expandToString("${alpha} ${beta} ${gamma} ${delta}"));
    }

    @org.junit.Test
    public void sysProp() {
        System.setProperty("alpha.beta", "eins zwei");
        expander.putAll("sys", System.getProperties());
        assertEquals("preeins zweipost",
                expander.expandToString("pre${!sys|alpha.beta}post"));
    }
}
