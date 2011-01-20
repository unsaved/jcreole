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
    private static File creoleInRoot = new File("test-data");
    private static File workOutRoot = new File("tmp/testwork");
    private static final String creoleInRootPath = creoleInRoot.getPath();
    private static String FSEP = System.getProperty("file.separator");

    public CreoleParseTest(
            File creoleFile, File htmlExpectFile, File htmlFile) {
        this.creoleFile = creoleFile;
        this.htmlExpectFile = htmlExpectFile;
        this.htmlFile = htmlFile;
    }

    @Parameters
    public static List<File[]> creoleFiles() throws IOException {
        if (!creoleInRoot.isDirectory())
            throw new IllegalStateException(
                    "Dir missing: " + creoleInRoot.getAbsolutePath());
        if (workOutRoot.exists()) FileUtils.deleteDirectory(workOutRoot);
        workOutRoot.mkdir();
        List<File[]> fileParams = new ArrayList<File[]>();
        File eFile;
        for (File f : FileUtils.listFiles(
                creoleInRoot, new String[] { "creole" }, true)) {
            eFile = new File(f.getParentFile(),
                    f.getName().replaceFirst("\\..*", "") + ".html");
            if (!eFile.isFile())
                throw new IOException("Missing expect file: "
                        + eFile.getAbsolutePath());
            fileParams.add(new File[] {
                f, eFile,
                new File(workOutRoot,
                f.getParentFile().equals(creoleInRoot)
                ? eFile.getName()
                : (f.getParent().substring(creoleInRootPath.length())
                        + FSEP + eFile.getName())
            )});
        }
        return fileParams;
    }

    @org.junit.Test
    public void parseTest() throws IOException {
        Object retVal = null;
        try {
            retVal = new CreoleParser().parse(new CreoleScanner(
                    new FileInputStream(creoleFile)));
        } catch (Exception e) {
            fail("Failed to parse '" + creoleFile + "': " + e);
        }
        FileUtils.writeStringToFile(htmlFile,
                ((retVal == null) ? ""
                                  : (((WashedToken) retVal).getCleanString()))
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
