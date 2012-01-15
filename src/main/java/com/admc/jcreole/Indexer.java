package com.admc.jcreole;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;
import java.util.Formatter;

public class Indexer {
    private Pattern namePattern;
    private String nameFormatString;
    private FileFilter filter;

    /**
     * @param filter  If null, then all files in the specified dir will be
     *        included.
     */
    public void setFilter(FileFilter filter) {
        this.filter = filter;
    }

    /**
     * @param nameTranslationMatchPat nameTranslation* params must both be null
     *        or both be non-null.
     */
    public void setNameTranslationMatchPat(String namePatternString) {
        namePattern = Pattern.compile(namePatternString);
    }

    /**
     * @param nameTranslationFormat nameTranslation* params must both be null
     *        or both be non-null.
     */
    public void setNameTranslationFormat(String nameFormatString) {
        this.nameFormatString = nameFormatString;
    }

    /**
     * @param directory  Will throw an IllegalArgumentException if this is not
     *        a real directory.
     * @param listUp  Generate an entry for "..".
     * @return HTML fragment that is a HTML table element.
     */
    public StringBuilder generateTable(File directory, boolean listUp) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        return null;
    }
}
