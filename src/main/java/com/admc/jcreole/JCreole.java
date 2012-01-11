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
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.admc.util.IOUtil;
import com.admc.util.Expander;

/**
 * Generates HTML fragments from supplied Creole wikitext, optionally making a
 * complete HTML page by merging the generated fragment with a HTML page
 * boilerplate.
 * <p>
 * Assembles HTML pages built around main content built from Creole wikitext.
 * </p><p>
 * Applications may use CreoleScanner and CreoleParser directly for more precise
 * control over how HTML pages are constructed.
 * One development strategy would be to start with a copy of the source code of
 * this class and modify it to fit with your application design and
 * technologies.
 * </p>
 *
 * @see CreoleScanner
 * @see CreoleParser
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.0
 */
public class JCreole {
    private static Log log = LogFactory.getLog(JCreole.class);

    private static final String DEFAULT_BP_RES_PATH = "boilerplate-inet.html";

    public static final String SYNTAX_MSG =
        "java -jar .../jcreole-*.jar [-d] [-] [-r /classpath/boiler.html] "
        + "[-f /fs/boiler.html] [-o fspath/out.html] pathto/input.creole\n\n"
        + "The -, -r, and -f options are mutually exclusive.\n"
        + "  NONE:    Default built-in boilerplate.\n"
        + "  -:       No boilerplate.  Output will be just a HTML fragment.\n"
        + "  -r path: Load specified boilerplate file from Classpath.\n"
        + "  -f path: Load specified boilerplate file from file system.\n"
        + "If either -r or -f is specified, the specified boilerplate should "
        + "include\n'${content}' at the point(s) where you want content "
        + "generated from your Creole\ninserted.\n"
        + "If the outputfile already exists, it will be silently "
        + "overwritten.\n"
        + "The input Creole file is sought first in the classpath "
        + "(relative to classpath\n"
        + "roots) then falls back to looking for a filesystem file.\n"
        + "The -d option loads an IntraWiki-link debug mapper.\n"
        + "Output is always written with UTF-8 encoding.";

    protected CreoleParser parser = new CreoleParser();
    private CharSequence pageBoilerPlate;
    private String pageTitle;
    private Expander expander;

    /**
     * Run this method with no parameters to see syntax requirements and the
     * available parameters.
     *
     * N.b. do not call this method from a persistent program, because it
     * may call System.exit!
     * <p>
     * Any long-running program should use the lower-level methods in this
     * class instead (or directly use CreoleParser and CreoleScanner
     * instances).
     * </p> <p>
     * This method executes with all JCreole privileges.
     * Variable references like ${this} in the Creole text will be expanded to
     * the corresponding Java system property values.
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
        boolean debugMapper = false;
        boolean noBp = false;
        int param = -1;
        try {
            while (++param < sa.length) {
                if (sa[param].equals("-d")) {
                    debugMapper = true;
                    continue;
                }
                if (sa[param].equals("-r") && param + 1 < sa.length) {
                    if (bpFsPath != null || bpResPath != null || noBp)
                            throw new IllegalArgumentException();
                    bpResPath = sa[++param];
                    continue;
                }
                if (sa[param].equals("-f") && param + 1 < sa.length) {
                    if (bpResPath != null || bpFsPath != null || noBp)
                            throw new IllegalArgumentException();
                    bpFsPath = sa[++param];
                    continue;
                }
                if (sa[param].equals("-")) {
                    if (noBp || bpFsPath != null || bpResPath != null)
                            throw new IllegalArgumentException();
                    noBp = true;
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
        } catch (IllegalArgumentException iae) {
            System.err.println(SYNTAX_MSG);
            System.exit(1);
        }
        if (!noBp && bpResPath == null && bpFsPath == null)
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
            rawBoilerPlate = IOUtil.toString(iStream);
        } else if (bpFsPath != null) {
            rawBoilerPlate = IOUtil.toString(new File(bpFsPath));
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
        JCreole jCreole = (rawBoilerPlate == null)
                ? (new JCreole()) : (new JCreole(rawBoilerPlate));
        Expander exp = new Expander();
        jCreole.setExpander(exp);
        exp.putAll(null, System.getProperties(), false);
        if (debugMapper) jCreole.setInterWikiMapper(new InterWikiMapper() {
            // This InterWikiMapper is just for prototyping.
            // Use wiki name of "nil" to force lookup failure for path.
            // Use wiki page of "nil" to force lookup failure for label.
            public String toPath(String wikiName, String wikiPage) {
                if (wikiName != null && wikiName.equals("nil")) return null;
                return "{WIKI-LINK to: " + wikiName + '|' + wikiPage + '}';
            }
            public String toLabel(String wikiName, String wikiPage) {
                if (wikiPage == null)
                        throw new RuntimeException(
                                "Null page name sent to InterWikiMapper");
                if (wikiPage.equals("nil")) return null;
                return "{LABEL for: " + wikiName + '|' + wikiPage + '}';
            }
        });
        jCreole.setPageTitle((inFile == null)
                ? creoleResPath.replaceFirst("[.][^.]*$", "")
                    .replaceFirst(".*[/\\\\.]", "")
                : inFile.getName().replaceFirst("[.][^.]*$", ""));
        jCreole.setPrivileges(EnumSet.allOf(JCreolePrivilege.class));
        String generatedHtml = (creoleStream == null)
                ? jCreole.parseCreole(inFile)
                : jCreole.parseCreole(IOUtil.toStringBuilder(creoleStream));
        String html = jCreole.postProcess(
                generatedHtml, SystemUtils.LINE_SEPARATOR);
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
        if (rawBoilerPlate.indexOf("${content}") < 0
                && rawBoilerPlate.indexOf("${!content}") < 0)
            throw new IllegalArgumentException(
                    "Boilerplate contains neither ${content} nor ${!content}");
        pageBoilerPlate = rawBoilerPlate.replace("\r", "");
    }

    /**
     * Returns a HTML <strong>FRAGMENT</strong> from the specified Creole
     * Wikitext.
     * You don't need to worry about \r's in input, as they will automatically
     * be stripped if present.
     * (The will, however, throw if binary characters are detected in input).
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
        if (sb == null || sb.length() < 1)
            throw new IllegalArgumentException("No input supplied");
        CreoleScanner scanner =
                CreoleScanner.newCreoleScanner(sb, true, expander);
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
            log.error("Unexpected problem.  Passing RuntimeException to caller",
                    rte);
            throw new CreoleParseException("Unexpected problem", rte);
        }
        if (!(retVal instanceof WashedSymbol)) {
            log.error("Parser returned unexpected type "
                    + retVal.getClass().getName() + ".  Throwing RTE.");
            throw new IllegalStateException(
                    "Parser returned unexpected type: "
                    + retVal.getClass().getName());
        }
        return ((WashedSymbol) retVal).toString();
    }

    /**
     * Returns a HTML <strong>FRAGMENT</strong> from the specified Creole
     * Wikitext file.
     * You don't need to worry about \r's in input, as they will automatically
     * be stripped if present.
     * (The will, however, throw if binary characters are detected in input).
     *
     * @throws if can not generate output, or if the run generates 0 output.
     *         If the problem is due to input formatting, in most cases you
     *         will get a CreoleParseException, which is a RuntimException, and
     *         CreoleParseException has getters for locations in the source
     *         data (though they will be offset for \r's in the provided
     *         Creole source, if any).
     */
    public String parseCreole(File creoleFile) throws IOException {
        if (creoleFile == null || creoleFile.length() < 1)
            throw new IllegalArgumentException("No input supplied");
        CreoleScanner scanner =
                CreoleScanner.newCreoleScanner(creoleFile, false, expander);
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
        if (!(retVal instanceof WashedSymbol))
            throw new IllegalStateException(
                    "Parser returned unexpected type: "
                    + retVal.getClass().getName());
        return ((WashedSymbol) retVal).toString();
    }

    /**
     * Generates clean HTML with specified EOL type.
     * If 'pageBoilerPlate' is set for this JCreole instance, then will return
     * an eol-converted merge of the page boilerplate (with substitutions) with
     * embedded HTML fragment.
     * Otherwise will just return the supplied fragment with the Eols converted
     * as necessary.
     * <p>
     * Call like <PRE>
     *    postProcess(htmlFrag, System.getProperty("line.separator"));
     * to have output match your local platform default.
     * </p> <p>
     * Input htmlFrag doesn't need to worry about line delimiters, because it
     * will be cleaned up as required.
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
    protected String postProcess(String htmlFrag, String outputEol)
            throws IOException {
        if (pageBoilerPlate == null) return
                (outputEol == null || outputEol.equals("\n"))
                ? htmlFrag : htmlFrag.replace("\n", outputEol);
                // Amazing that StringBuilder can't do a multi-replace like this

        Expander bpExpander = new Expander();
        StringBuilder html = new StringBuilder(pageBoilerPlate);
        if (html.indexOf("${headers}") > -1
                || html.indexOf("${!headers}") > -1) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String href : getCssHrefs())
                sb.append(String.format("<link id=\"auto%02d\" class=\"auto\" "
                        + "rel=\"stylesheet\" "
                        + "type=\"text/css\" href=\"%s\" />\n", ++count, href));
            bpExpander.put("headers", sb.toString(), false);
        } else if (getCssHrefs().size() > 0) {
            throw new CreoleParseException(
                    "Author-supplied style-sheets, but boilerplate has no "
                    + "'headers' insertion-point");
        }
        bpExpander.put("timeStamp",
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .format(new Date()), false);
        bpExpander.put("content", htmlFrag, false);
        if (pageTitle != null) bpExpander.put("pageTitle", pageTitle, false);
        String htmlString = bpExpander.expand(html).toString();
        return (outputEol == null || outputEol.equals("\n"))
                ? htmlString : htmlString.replace("\n", outputEol);
                // Amazing that StringBuilder can't do a multi-replace like this
    }

    /**
     * Will use the specified Expander when instantiating the scanner.
     *
     * @see CreoleScanner.newCreoleScanner(File, boolean, Expander);
     * @see CreoleScanner.newCreoleScanner(StringBuilder, boolean, Expander);
     */
    public void setExpander(Expander expander) {
        this.expander = expander;
    }

    /**
     * Calls the corresponding method on the underlying Parser.
     *
     * @see CreoleParser#setPrivileges(EnumSet)
     */
    public void setPrivileges(EnumSet<JCreolePrivilege> jcreolePrivs) {
        parser.setPrivileges(jcreolePrivs);
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
     * @see CreoleParser#getPrivileges()
     */
    public EnumSet<JCreolePrivilege> getPrivileges() {
        return parser.getPrivileges();
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
