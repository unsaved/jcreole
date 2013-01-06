package com.admc.jcreole;

import java.io.File;
import java.io.FileFilter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import org.apache.commons.lang.StringEscapeUtils;
import java.text.SimpleDateFormat;
import com.admc.util.FileComparator;

public class Indexer {
    private Pattern namePattern;
    private String nameFormatString;
    private FileFilter filter;
    
    private static SimpleDateFormat isoDateTimeFormatter =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private static Pattern tailStripperPattern = Pattern.compile("[^/]+$");

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
     * This is a highly-modified fork of Tomcat's method
     * DefaultServlet.renderHtml().
     *
     * @param directory  Will throw an IllegalArgumentException if this is not
     *        a real directory.
     * @param listUp  Generate an entry for "..".
     * @return HTML fragment that is a HTML table element.
     */
    public StringBuilder generateTable(File directory, String displayName,
            boolean listUp, FileComparator.SortBy sortBy, boolean ascendSort) {
        /*
         * TODO: Use an EnumMap or something to cache FileComparators instead
         * of instantiating one for every method call.
         */
        Matcher matcher = null;
        if (!directory.isDirectory())
            throw new IllegalArgumentException(
                    "Not a directory:  " + directory.getAbsolutePath());
        if ((namePattern == null && nameFormatString != null)
                || (namePattern != null && nameFormatString == null))
            throw new IllegalStateException(
                    "'namePattern' and 'nameFormatString' must either both be "
                    + "set or both be null");
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        String name = displayName;

        sb.append("<table class=\"jcreole_dirindex\" width=\"100%\" "
                + "cellspacing=\"0\" cellpadding=\"5\" align=\"center\">\r\n");

        // Render the column headings
        sb.append("<tr>\r\n");
        sb.append("<td align=\"left\"><font size=\"+1\"><strong>");
        sb.append("<a href=\"?sort=");
        try {
        sb.append(URLEncoder.encode(
                (sortBy == FileComparator.SortBy.NAME && ascendSort)
                ? "-" : "+", "UTF-8"));
        sb.append("NAME\">Nodename</a>");
        if (sortBy == FileComparator.SortBy.NAME)
            sb.append("<sup>").append(ascendSort ? '+' : '-').append("</sup>");
        sb.append("</strong></font></td>\r\n");
        sb.append("<td align=\"center\"><font size=\"+1\"><strong>");
        sb.append("<a href=\"?sort=");
        sb.append(URLEncoder.encode(
                (sortBy == FileComparator.SortBy.SIZE && ascendSort)
                ? "-" : "+", "UTF-8"));
        sb.append("SIZE\">Size</a>");
        if (sortBy == FileComparator.SortBy.SIZE)
            sb.append("<sup>").append(ascendSort ? '+' : '-').append("</sup>");
        sb.append("</strong></font></td>\r\n");
        sb.append("<td align=\"right\"><font size=\"+1\"><strong>");
        sb.append("<a href=\"?sort=");
        sb.append(URLEncoder.encode(
                (sortBy == FileComparator.SortBy.MODIFIED && ascendSort)
                ? "-" : "+", "UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Unable to encode to UTF-8");
        }
        sb.append("MODIFIED\">Last Modified</a>");
        if (sortBy == FileComparator.SortBy.MODIFIED)
            sb.append("<sup>").append(ascendSort ? '+' : '-').append("</sup>");
        sb.append("</strong></font></td>\r\n");
        sb.append("</tr>");

        boolean shade = true;

        // DIR ENTRY:
        if (listUp) {
            sb.append("<tr");
            sb.append(" bgcolor=\"#eeeeee\"");
            sb.append(">\r\n");

            sb.append("<td align=\"left\">&nbsp;&nbsp;\r\n");
            sb.append("<a href=\"../\"><tt><strong>..</strong>&nbsp;&nbsp;(");
            sb.append(StringEscapeUtils.escapeHtml(
                    tailStripperPattern.matcher(displayName).replaceFirst("")));
            sb.append(")</tt></a></td>\r\n");

            sb.append("<td align=\"right\"><tt>");
            sb.append("&nbsp;");
            sb.append("</tt></td>\r\n");

            sb.append("<td align=\"right\"><tt>");
            sb.append(StringEscapeUtils.escapeHtml(isoDateTimeFormatter.format(
                    directory.getParentFile().lastModified())));
            sb.append("</tt></td>\r\n");

            sb.append("</tr>\r\n");
        }  // END DIR ENTRY

        List<File> fileList = Arrays.asList(directory.listFiles(filter));
        Collections.sort(fileList, new FileComparator(sortBy, ascendSort));

        // Render the directory entries within this directory
        for (File file : fileList) {
            String nodeName = file.getName();
            if (namePattern != null)  {
                if (file.isFile()) {
                    matcher = namePattern.matcher(nodeName);
                    if (!matcher.matches()) continue;
                } else {
                    matcher = null;
                }
            }

            shade = !shade;
            sb.append("<tr");
            if (shade) sb.append(" bgcolor=\"#eeeeee\"");
            sb.append(">\r\n");

            sb.append("<td align=\"left\">&nbsp;&nbsp;\r\n");
            sb.append("<a href=\"");
            if (matcher == null) {
                sb.append(nodeName);
            } else {
                // Terrible hack dur to terrible Java varargs limitation:
                switch (matcher.groupCount()) {
                  case 1:
                    formatter.format(nameFormatString, matcher.group(1));
                    break;
                  case 2:
                    formatter.format(nameFormatString, matcher.group(1),
                            matcher.group(2));
                    break;
                  case 3:
                    formatter.format(nameFormatString, matcher.group(1),
                            matcher.group(2), matcher.group(3));
                    break;
                  case 4:
                    formatter.format(nameFormatString, matcher.group(1),
                            matcher.group(2), matcher.group(3),
                            matcher.group(4));
                    break;
                  case 5:
                    formatter.format(nameFormatString, matcher.group(1),
                            matcher.group(2), matcher.group(3),
                            matcher.group(4), matcher.group(5));
                    break;
                  case 6:
                    formatter.format(nameFormatString, matcher.group(1),
                            matcher.group(2), matcher.group(3),
                            matcher.group(4), matcher.group(5),
                            matcher.group(6));
                    break;
                  case 7:
                    formatter.format(nameFormatString, matcher.group(1),
                            matcher.group(2), matcher.group(3),
                            matcher.group(4), matcher.group(5),
                            matcher.group(6), matcher.group(7));
                    break;
                  case 8:
                    formatter.format(nameFormatString, matcher.group(1),
                            matcher.group(2), matcher.group(3),
                            matcher.group(4), matcher.group(5),
                            matcher.group(6), matcher.group(7),
                            matcher.group(8));
                    break;
                  case 9:
                    formatter.format(nameFormatString, matcher.group(1),
                            matcher.group(2), matcher.group(3),
                            matcher.group(4), matcher.group(5),
                            matcher.group(6), matcher.group(7),
                            matcher.group(8), matcher.group(9));
                    break;
                  default:
                    throw new IllegalArgumentException(
                            "Pattern captured too many (" + matcher.groupCount()
                            + ") groups: " + namePattern);
                }
            }
            if (file.isDirectory()) sb.append('/');
            sb.append("\"><tt>");
            sb.append(StringEscapeUtils.escapeHtml(nodeName));
            if (file.isDirectory()) sb.append('/');
            sb.append("</tt></a></td>\r\n");

            sb.append("<td align=\"right\"><tt>");
            if (file.isDirectory()) sb.append("&nbsp;");
            else if (file.isFile()) sb.append(file.length());
            sb.append("</tt></td>\r\n");

            sb.append("<td align=\"right\"><tt>");
            sb.append(StringEscapeUtils.escapeHtml(
                    isoDateTimeFormatter.format(file.lastModified())));
            sb.append("</tt></td>\r\n");

            sb.append("</tr>\r\n");
        }

        // Render the page footer
        sb.append("</table>\r\n");

        return sb;
    }
}
