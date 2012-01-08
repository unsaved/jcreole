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

/**
 * A JUnit unit test.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 */
class ExpanderTest {
    private Expander expander = new Expander();

    @org.junit.Test
    void noExpands() {
        expander.putAll([alpha: 'one', beta: 'two'])
        assertEquals("one, two\nthree, four",
                expander.expand("one, two\nthree, four"))
    }

    @org.junit.Test
    void noMatches() {
        expander.putAll([alpha: 'one', beta: 'two'])
        assertEquals("one, ${unset1}, two\nthree, ${unset2}, four",
                expander.expand("one, ${unset1}, two\nthree, ${unset2}, four"))
    }

    @org.junit.Test(expected=GradleException.class)
    void illegalNs() {
        expander.expand("abc-def", "one, two\nthree, four")
    }

    @org.junit.Test
    void keyRenames() {
        assertEquals([('al-pha'): 'al_pha', (' gamma'), '_gamma'],
                expander.putAll(
                [('al-pha'): 'one', beta: 'two', (' gamma'), 'three']))
    }
}
