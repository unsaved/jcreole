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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;
import org.apache.commons.io.FileUtils;

@RunWith(value = Parameterized.class)
/**
 * A JUnit unit test.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.0
 */
public class CreoleParseTest {
    private File creoleFile, htmlExpectFile, htmlFile;
    private boolean shouldSucceed;
    private static File pCreoleInRoot = new File("src/test/data/positive");
    private static File pWorkOutRoot = new File("build/test-results/positive");
    private static final String pCreoleInRootPath = pCreoleInRoot.getPath();
    private static File nCreoleInRoot = new File("src/test/data/negative");
    private static File nWorkOutRoot = new File("build/test-results/negative");
    private static final String nCreoleInRootPath = nCreoleInRoot.getPath();
    private static String FSEP = System.getProperty("file.separator");

    public CreoleParseTest(File creoleFile,
            File htmlExpectFile, File htmlFile, Boolean doSucceed) {
        this.creoleFile = creoleFile;
        this.htmlExpectFile = htmlExpectFile;
        this.htmlFile = htmlFile;
        shouldSucceed = doSucceed.booleanValue();
    }

    @Parameters
    public static List<Object[]> creoleFiles() throws IOException {
        if (!pCreoleInRoot.isDirectory())
            throw new IllegalStateException(
                    "Dir missing: " + pCreoleInRoot.getAbsolutePath());
        if (!nCreoleInRoot.isDirectory())
            throw new IllegalStateException(
                    "Dir missing: " + nCreoleInRoot.getAbsolutePath());
        if (pWorkOutRoot.exists()) FileUtils.deleteDirectory(pWorkOutRoot);
        pWorkOutRoot.mkdir();
        if (nWorkOutRoot.exists()) FileUtils.deleteDirectory(nWorkOutRoot);
        nWorkOutRoot.mkdir();
        List<Object[]> params = new ArrayList<Object[]>();
        File eFile;
        for (File f : FileUtils.listFiles(
                pCreoleInRoot, new String[] { "creole" }, true)) {
            eFile = new File(f.getParentFile(),
                    f.getName().replaceFirst("\\..*", "") + ".html");
            params.add(new Object[] {
                f, eFile,
                (eFile.isFile()
                ? new File(pWorkOutRoot,
                f.getParentFile().equals(pCreoleInRoot)
                ? eFile.getName()
                : (f.getParent().substring(pCreoleInRootPath.length())
                        + FSEP + eFile.getName())) : null),
                Boolean.TRUE
            });
        }
        String name;
        for (File f : FileUtils.listFiles(
                nCreoleInRoot, new String[] { "creole" }, true)) {
            name = f.getName().replaceFirst("\\..*", "") + ".html";
            params.add(new Object[] {
                f, null,
                new File(nWorkOutRoot,
                f.getParentFile().equals(nCreoleInRoot)
                ? name
                : (f.getParent().substring(nCreoleInRootPath.length())
                        + FSEP + name)),
                Boolean.FALSE
            });
        }
        return params;
    }

    @org.junit.Test
    public void parseTest() throws IOException {
        if (htmlExpectFile != null)
            assertNotNull("Missing expect file: "
                    + htmlExpectFile.getAbsolutePath(), htmlFile);
        Object retVal = null;
        try {
            CreoleParser parser = new CreoleParser();
            parser.setPrivileges(EnumSet.allOf(JCreolePrivilege.class));
            /* Replace the statement above with something like this to test
             * privileges:
            parser.setPrivileges(EnumSet.of(
                    JCreolePrivilege.ENUMFORMATS,
                    JCreolePrivilege.TOC,
                    JCreolePrivilege.RAWHTML,
                    JCreolePrivilege.STYLESHEET,
                    JCreolePrivilege.JCXBLOCK,
                    JCreolePrivilege.JCXSPAN,
                    JCreolePrivilege.STYLER
            ));
            */
            parser.setInterWikiMapper(new InterWikiMapper() {
                // Use wiki name of "Nil" to force lookup failure for path.
                public String toPath(String wikiName, String wikiPage) {
                    if (wikiName != null && wikiName.equals("Nil")) return null;
                    return "{WIKI-LINK to: " + wikiName + '|' + wikiPage + '}';
                }
                // Use wiki page of "nil" to force lookup failure for label.
                public String toLabel(String wikiName, String wikiPage) {
                    if (wikiPage == null)
                        throw new RuntimeException(
                                "Null page name sent to InterWikiMapper");
                    if (wikiPage.equals("nil")) return null;
                    return "{LABEL for: " + wikiName + '|' + wikiPage + '}';
                }
            });
            retVal = parser.parse(
                    CreoleScanner.newCreoleScanner(creoleFile, false, null));
        } catch (Exception e) {
            if (!shouldSucceed) return;  // A ok.  No output file to write.
            AssertionError ae =
                    new AssertionError("Failed to parse '" + creoleFile + "'");
            ae.initCause(e);
            throw ae;
        }
        FileUtils.writeStringToFile(htmlFile,
                ((retVal == null) ? "" : (((WashedSymbol) retVal).toString()))
                , "UTF-8");
        if (!shouldSucceed)
            fail("Should have failed, but generated '" + htmlFile + "'");
        assertTrue("From '" + creoleFile + "':  '" + htmlFile
                + "' != '" + htmlExpectFile + "'",
            FileUtils.contentEquals(htmlExpectFile, htmlFile));
        htmlFile.delete();
    }


    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(CreoleParseTest.class.getName());
    }
}
