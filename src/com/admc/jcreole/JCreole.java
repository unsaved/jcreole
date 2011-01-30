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

import java.util.EnumSet;
import java.io.IOException;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Assembles HTML pages built around main content built from Creole wikitext.
 *
 * Applications may use CreoleScanner and CreoleParser directly for more precise
 * control over how HTML pages are constructed.
 * One development strategy would be to start with a copy of the source code of
 * this class and modify it to fit with your application design and
 * technologies.
 *
 * @see CreoleScanner
 * @see CreoleParser
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.0
 */
public class JCreole {
    private static Log log = LogFactory.getLog(JCreole.class);

    private static final String DEFAULT_BP_RES_PATH =
            "boilerplate-default.html";

    public static final String SYNTAX_MSG =
            "java -jar .../jcreole-*.jar [-r /classpath/boiler.html] "
            + "[-f /fs/boiler.html] [-o fspath/out.html] fspath/input.creole\n"
            + "Where either classpath or filesystem boiler plate page includes "
            + "'${content}' at the point(s)\n"
            + "where you want content generated from your Creole inserted.\n"
            + "The -r and -f options are mutually exclusive.\n\n"
            + "Defaults are to use the default built-in boilerplate and to "
            + "write generated page to stdout.\n"
            + "If the outputfile already exists, it will be silently "
            + "overwritten\n"
            + "Output is always written with UTF-8 encoding.";

    protected CreoleParser parser = new CreoleParser();
    private String pageBoilerPlate;
    private String pageTitle;

    /**
     * Run this method with no parameters to see syntax requirements and the
     * available parameters.
     *
     * N.b. do not call this method from a persistent program, because it
     * calls System.exit!
     * <p>
     * Any long-running program should use one of the lower-level methods.
     * </p>
     *
     * @throws IOException for any I/O problem that makes it impossible to
     *         satisfy the request.
     * @throws CreoleParseException
     *         if can not generate output, or if the run generates 0 output.
     *         If the problem is due to input formatting, in most cases you
     *         will get a CreoleParseException, which is a RuntimException, and
     *         CreoleParseException has getters for locations in the source
     *         data (though they will be offset for \r's in the provided
     *         Creole source, if any).
     */
    public static void main(String[] sa) throws IOException {
        String bpResPath = null;
        String bpFsPath = null;
        String outPath = null;
        String inPath = null;
        int param = -1;
        try {
            while (++param < sa.length) {
                if (sa[param].equals("-r") && param + 1 < sa.length) {
                    if (bpResPath != null)
                            throw new IllegalArgumentException();
                    bpResPath = sa[++param];
                    continue;
                }
                if (sa[param].equals("-f") && param + 1 < sa.length) {
                    if (bpFsPath != null) throw new IllegalArgumentException();
                    bpFsPath = sa[++param];
                    continue;
                }
                if (sa[param].equals("-o") && param + 1 < sa.length) {
                    if (outPath != null) throw new IllegalArgumentException();
                    outPath = sa[++param];
                    continue;
                }
                if (inPath != null) throw new IllegalArgumentException();
                inPath = sa[param];
            }
            if (inPath == null) throw new IllegalArgumentException();
            if (bpResPath != null && bpFsPath != null)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException iae) {
            System.err.println(SYNTAX_MSG);
            System.exit(1);
        }
        if (bpResPath == null && bpFsPath == null)
            bpResPath = DEFAULT_BP_RES_PATH;
        String rawBoilerPlate = null;
        if (bpResPath != null) {
            if (bpResPath.length() > 0 && bpResPath.charAt(0) == '/')
                // Classloader lookups are ALWAYS relative to CLASSPATH roots,
                // and will abort if you specify a beginning "/".
                bpResPath = bpResPath.substring(1);
            InputStream iStream = Thread.currentThread()
                    .getContextClassLoader().getResourceAsStream(bpResPath);
            if (iStream == null)
                throw new IOException("Boilerplate inaccessible: " + bpResPath);
            rawBoilerPlate = IOUtils.toString(iStream, "UTF-8");
        } else if (bpFsPath != null) {
            rawBoilerPlate =
                    FileUtils.readFileToString(new File(bpFsPath), "UTF-8");
        } else {
            throw new RuntimeException(
                    "Internal error.  Neither bpResPath " + "nor bpFsPath set");
        }
        File inFile = new File(inPath);
        JCreole jCreole = new JCreole(rawBoilerPlate);
        jCreole.setPageTitle(inFile.getName().replaceFirst("[.][^.]*", ""));
        jCreole.setPluginPrivileges(
                EnumSet.complementOf(EnumSet.of(PluginPrivilege.RAWHTML)));
        String html = jCreole.generateHtmlPage(
                inFile, SystemUtils.LINE_SEPARATOR);
        if (outPath == null) {
            System.out.print(html);
        } else {
            FileUtils.writeStringToFile(new File(outPath), html, "UTF-8");
        }
    }

    public JCreole(String rawBoilerPlate) {
        if (rawBoilerPlate.indexOf("${content}") < 0)
            throw new IllegalArgumentException(
                    "Boilerplate text does not contain '${content}'");
        pageBoilerPlate = rawBoilerPlate.replace("\r", "");
    }

    /**
     * Returns a HTML <strong>FRAGMENT</strong> from the specified Creole
     * Wikitext.
     *
     * @throws if can not generate output, or if the run generates 0 output.
     *         If the problem is due to input formatting, in most cases you
     *         will get a CreoleParseException, which is a RuntimException, and
     *         CreoleParseException has getters for locations in the source
     *         data (though they will be offset for \r's in the provided
     *         Creole source, if any).
     */
    public String parseCreole(StringBuilder sb) throws IOException {
        CreoleScanner scanner = CreoleScanner.newCreoleScanner(sb, false);
        // using a named instance so we can enhance this to set scanner
        // instance properties.
        Object retVal = null;
        try {
            retVal = parser.parse(scanner);
        } catch (CreoleParseException cpe) {
            throw cpe;
        } catch (beaver.Parser.Exception bpe) {
            throw new CreoleParseException(bpe);
        }
        if (retVal == null)
            throw new IllegalArgumentException("Input generated no output");
        if (!(retVal instanceof WashedSymbol))
            throw new IllegalStateException(
                    "Parser returned unexpected type: "
                    + retVal.getClass().getName());
        return ((WashedSymbol) retVal).toString();
    }

    /**
     * Returns a HTML <strong>FRAGMENT</strong> from the specified Creole
     * Wikitext file.
     *
     * @throws if can not generate output, or if the run generates 0 output.
     *         If the problem is due to input formatting, in most cases you
     *         will get a CreoleParseException, which is a RuntimException, and
     *         CreoleParseException has getters for locations in the source
     *         data (though they will be offset for \r's in the provided
     *         Creole source, if any).
     */
    public String parseCreole(File creoleFile) throws IOException {
        CreoleScanner scanner =
                CreoleScanner.newCreoleScanner(creoleFile, false);
        // using a named instance so we can enhance this to set scanner
        // instance properties.
        Object retVal = null;
        try {
            retVal = parser.parse(scanner);
        } catch (CreoleParseException cpe) {
            throw cpe;
        } catch (beaver.Parser.Exception bpe) {
            throw new CreoleParseException(bpe);
        }
        if (retVal == null)
            throw new IllegalArgumentException("Input generated no output");
        if (!(retVal instanceof WashedSymbol))
            throw new IllegalStateException(
                    "Parser returned unexpected type: "
                    + retVal.getClass().getName());
        return ((WashedSymbol) retVal).toString();
    }

    /**
     * Generates HTML page with specified EOL type.
     * <p>
     * Call like <PRE>
     *    generateHtmlPage(bpStr, cFile, System.getProperty("line.separator"));
     * to have output match your local platform default.
     * </p> <p>
     *  Input doesn't need to worry about line delimiters, because it will be
     *  cleaned up as required.
     * </p>
     *
     * @param outputEol  Line delimiters for output.
     * @throws if can not generate output, or if the run generates 0 output.
     *         If the problem is due to input formatting, in most cases you
     *         will get a CreoleParseException, which is a RuntimException, and
     *         CreoleParseException has getters for locations in the source
     *         data (though they will be offset for \r's in the provided
     *         Creole source, if any).
     */
    public String generateHtmlPage(File creoleFile, String outputEol)
            throws IOException {
        String gendHtml = parseCreole(creoleFile);
        StringBuilder html = new StringBuilder(pageBoilerPlate);
        int index = html.indexOf("${content}");
        html.replace(index, index + "${content}".length(), gendHtml);
        index = html.indexOf("${headers}");
        if (index > -1)
            html.replace(index, index + "${headers}".length(),
                    parser.getHeaderInsertion());
        index = html.indexOf("${pageTitle}");
        if (pageTitle != null && index > -1)
            html.replace(index, index + "${pageTitle}".length(), pageTitle);
        return (outputEol == null || outputEol.equals("\n"))
                ? html.toString() : html.toString().replace("\n", outputEol);
                // Amazing that StringBuilder can't do a multi-replace like this
    }

    /**
     * Specify what plugin directives may be used by Creole page authors.
     */
    public void setPluginPrivileges(EnumSet<PluginPrivilege> pluginPrivs) {
        parser.setPluginPrivileges(pluginPrivs);
    }

    /**
     * Set enumeration formats to display section/heading enumerations in TOCs
     * (if any) and in headings.
     * <p>
     * By default, no section/header enumerations are shown.
     * Creole authors can alternatively use the &lt;&lt;enumFormats&gt;&gt;
     * plugin directive to accomplish the same thing (assuming that they have
     * adequate privileges to do so).
     * </p>
     */
    public void setEnumerationFormats(String enumerationFormats) {
        parser.setEnumerationFormats(enumerationFormats);
    }

    /**
     * @see #setPluginPrivileges
     */
    public EnumSet<PluginPrivilege> getPluginPrivileges() {
        return parser.getPluginPrivileges();
    }

    /**
     * Set what HTML page title will be displayed if your boilerplate makes
     * use of ${pageTitle}.
     */
    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }
}
