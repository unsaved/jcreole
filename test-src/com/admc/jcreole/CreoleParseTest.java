package com.admc.jcreole;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
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
public class CreoleParseTest {
    private File creoleFile, htmlExpectFile, htmlFile;
    private boolean shouldSucceed;
    private static File pCreoleInRoot = new File("test-data/positive");
    private static File pWorkOutRoot = new File("tmp/test-work/positive");
    private static final String pCreoleInRootPath = pCreoleInRoot.getPath();
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
        if (pWorkOutRoot.exists()) FileUtils.deleteDirectory(pWorkOutRoot);
        pWorkOutRoot.mkdir();
        List<Object[]> params = new ArrayList<Object[]>();
        File eFile;
        for (File f : FileUtils.listFiles(
                pCreoleInRoot, new String[] { "creole" }, true)) {
            eFile = new File(f.getParentFile(),
                    f.getName().replaceFirst("\\..*", "") + ".html");
            if (!eFile.isFile())
                throw new IOException("Missing expect file: "
                        + eFile.getAbsolutePath());
            params.add(new Object[] {
                f, eFile,
                new File(pWorkOutRoot,
                f.getParentFile().equals(pCreoleInRoot)
                ? eFile.getName()
                : (f.getParent().substring(pCreoleInRootPath.length())
                        + FSEP + eFile.getName())),
                Boolean.TRUE
            });
        }
        return params;
    }

    @org.junit.Test
    public void parseTest() throws IOException {
        Object retVal = null;
        try {
            retVal = new CreoleParser().parse(new CreoleScanner(
                    new FileInputStream(creoleFile)));
        } catch (Exception e) {
            AssertionError ae =
                    new AssertionError("Failed to parse '" + creoleFile + "'");
            ae.initCause(e);
            throw ae;
        }
        FileUtils.writeStringToFile(htmlFile,
                ((retVal == null) ? "" : (((WashedToken) retVal).toString()))
                , "UTF-8");
        assertTrue("From '" + creoleFile + "':  '" + htmlFile
                + "' != '" + htmlExpectFile + "'",
            FileUtils.contentEquals(htmlExpectFile, htmlFile));
        htmlFile.delete();
    }


    public static void main(String args[]) {
      org.junit.runner.JUnitCore.main(CreoleParseTest.class.getName());
    }
}
