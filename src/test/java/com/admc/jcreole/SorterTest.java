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
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;

@RunWith(value = Parameterized.class)
/**
 * A JUnit unit test.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class SorterTest {
    Comparator<String> comparator;
    String[] expected;

    String[] inputArr = new String[] {
        "513",
        "der Eins", "die Eins", "Eins", "das Eins",
        "Der Zwei", "Die Zwei", "Zwei", "Das Zwei",
        "deR Eins", "die EinS", "Ein", "das Eins "
    };

    public SorterTest(Comparator<String> comparator, String[] expected) {
        this.comparator = comparator;
        this.expected = expected;
    }

    @Parameters
    public static List<Object[]> creoleFiles() {
        List<Object[]> params = new ArrayList<Object[]>();
        params.add(new Object[] { null, new String[] {
            "513",
            "Das Zwei",
            "Der Zwei",
            "Die Zwei",
            "Ein",
            "Eins",
            "Zwei",
            "das Eins",
            "das Eins ",
            "deR Eins",
            "der Eins",
            "die EinS",
            "die Eins"
        }});

        params.add(new Object[] { new DictionaryComparator(), new String[] {
            "513",
            "das Eins",
            "das Eins ",
            "Das Zwei",
            "der Eins",
            "deR Eins",
            "Der Zwei",
            "die Eins",
            "die EinS",
            "Die Zwei",
            "Ein",
            "Eins",
            "Zwei"
        }});
        return params;
    }

    @org.junit.Test
    public void parseTest() {
        String[] actual = new String[inputArr.length];
        System.arraycopy(inputArr, 0, actual, 0, inputArr.length);
        Arrays.sort(actual, comparator);
        assertArrayEquals(((comparator == null)
                ? "<NULL>" : comparator.getClass().getName()) + " comparator",
                expected, actual);
    }


    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(SorterTest.class.getName());
    }
}
