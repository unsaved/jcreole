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
import java.util.List;
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
            + "[-f /fs/boiler.html] [-o fspath/out.html] pathto/input.creole\n"
            + "Where either classpath or filesystem boiler plate page includes "
            + "'${content}' at the point(s)\n"
            + "where you want content generated from your Creole inserted.\n"
            + "The -r and -f options are mutually exclusive.\n\n"
            + "Defaults are to use the default built-in boilerplate and to "
            + "write generated page to stdout.\n"
            + "If the outputfile already exists, it will be silently "
            + "overwritten\n"
            + "The input Creole file is sought first in the classpath "
            + "(relative to classpath roots)\n"
            + "then falls back to looking for a filesystem file.\n"
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
        String creoleResPath =
                (inPath.length() > 0 && inPath.charAt(0) == '/')
                ? inPath.substring(1)
                : inPath;
            // Classloader lookups are ALWAYS relative to CLASSPATH roots,
            // and will abort if you specify a beginning "/".
        InputStream creoleStream = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream(creoleResPath);
        File inFile = (creoleStream == null) ? new File(inPath) : null;
        JCreole jCreole = new JCreole(rawBoilerPlate);
        jCreole.setInterWikiMapper(new InterWikiMapper() {
            // This InterWikiMapper is just for prototyping.
            public String toPath(String wikiName, String wikiPage) {
                return "{WIKI-LINK to: " + wikiName + '/' + wikiPage + '}';
            }
            public String toLabel(String wikiName, String wikiPage) {
                return "{LABEL for: " + wikiName + '/' + wikiPage + '}';
            }
        });
        jCreole.setPageTitle((inFile == null)
                ? creoleResPath.replaceFirst("[.][^.]*$", "")
                    .replaceFirst(".*[/\\\\.]", "")
                : inFile.getName().replaceFirst("[.][^.]*$", ""));
        jCreole.setPluginPrivileges(
                EnumSet.complementOf(EnumSet.of(PluginPrivilege.RAWHTML)));
        String html = (creoleStream == null)
                ? jCreole.generateHtmlPage(inFile, SystemUtils.LINE_SEPARATOR)
                : jCreole.generateHtmlPage(
                        creoleStream, SystemUtils.LINE_SEPARATOR);
        if (outPath == null) {
            System.out.print(html);
        } else {
            FileUtils.writeStringToFile(new File(outPath), html, "UTF-8");
        }
    }

    /**
     * Use this when you want to work with HTML fragments externally.
     * Use the JCreole(String) constructor instead if you want JCreole to
     * manage HTML page construction with a Boilerplate.
     */
    public JCreole() {
        // Intentionally empty
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
     * @throws CreoleParseException
     *         if can not generate output, or if the run generates 0 output.
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
        } catch (RuntimeException rte) {
            throw new CreoleParseException("Unexpected problem", rte);
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
        return postProcess(parseCreole(creoleFile), outputEol);
    }

    /**
     * Just like the generateHtmlPage(File, String) method, but gets the
     * Creole input from the supplied input stream.
     *
     * @see #generateHtmlPage(File, String)
     */
    public String generateHtmlPage(InputStream creoleStream, String outputEol)
            throws IOException {
        return postProcess(parseCreole(
                new StringBuilder(IOUtils.toString(creoleStream, "UTF-8"))),
                outputEol);
    }

    /**
     * Wraps supplied HTML in a nice frame.
     */
    protected String postProcess(String gendHtml, String outputEol)
            throws IOException {
        if (pageBoilerPlate == null)
            throw new IllegalStateException(
                    "postProcess method requires the JCreole constructor "
                    + "that takes a Boilerplate");

        StringBuilder html = new StringBuilder(pageBoilerPlate);
        int index = html.indexOf("${content}");
        html.replace(index, index + "${content}".length(), gendHtml);
        index = html.indexOf("${headers}");
        if (index > -1) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String href : getCssHrefs())
                sb.append(String.format("<link id=\"auto%02d\" class=\"auto\" "
                        + "rel=\"stylesheet\" "
                        + "type=\"text/css\" href=\"%s\" />\n", ++count, href));
            html.replace(index, index + "${headers}".length(), sb.toString());
        } else {
            if (getCssHrefs().size() > 0)
                throw new CreoleParseException(
                    "Author-supplied style-sheets, but boilerplate has no "
                    + "'headers' insertion-point");
        }
        index = html.indexOf("${pageTitle}");
        if (pageTitle != null && index > -1)
            html.replace(index, index + "${pageTitle}".length(), pageTitle);
        return (outputEol == null || outputEol.equals("\n"))
                ? html.toString() : html.toString().replace("\n", outputEol);
                // Amazing that StringBuilder can't do a multi-replace like this
    }

    /**
     * Calls the corresponding method on the underlying Parser.
     *
     * @see CreoleParser#setPluginPrivileges(EnumSet)
     */
    public void setPluginPrivileges(EnumSet<PluginPrivilege> pluginPrivs) {
        parser.setPluginPrivileges(pluginPrivs);
    }

    /**
     * Calls the corresponding method on the underlying Parser.
     *
     * @see CreoleParser#setEnumerationFormats(String)
     */
    public void setEnumerationFormats(String enumerationFormats) {
        parser.setEnumerationFormats(enumerationFormats);
    }

    /**
     * Calls the corresponding method on the underlying Parser.
     *
     * @see CreoleParser#getPluginPrivileges()
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

    /**
     * Calls the corresponding method on the underlying Parser.
     *
     * @see CreoleParser#setInterWikiMapper(InterWikiMapper)
     */
    public void setInterWikiMapper(InterWikiMapper interWikiMapper) {
        parser.setInterWikiMapper(interWikiMapper);
    }

    /**
     * Gets the underlying Parser, with which you can do a lot of useful stuff.
     *
     * @see CreoleParser
     */
    public CreoleParser getParser() {
        return parser;
    }

    /**
     * Calls the corresponding method on the underlying Parser.
     *
     * @see CreoleParser#getCssHrefs
     */
    public List<String> getCssHrefs() {
        return parser.getCssHrefs();
    }
}
