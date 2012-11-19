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

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import com.admc.util.IOUtil;

/**
 * A JUnit unit test.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.2.0
 */
public class FullPageTest {
    @org.junit.Test
    public void pageTest() throws IOException {
        File outFile = new File("build/test-results/fullpage/page.html");
        File inCreoleFile = new File("src/test/data/fullpage/in/test.creole");
        File boilerplateFile = new File("src/test/data/fullpage/in/bplate.html");
        File expectFile = new File("src/test/data/fullpage/expect/page.html");
        (new File("build/test-results/fullpage")).mkdir();
        JCreole.main(new String[] {
                "-d", "-o", outFile.getPath(), "-f", boilerplateFile.getPath(),
                inCreoleFile.getPath()});
        assertEquals("HTML output wrong",
                IOUtil.toString(expectFile), IOUtil.toString(outFile));
    }


    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(FullPageTest.class.getName());
    }
}
