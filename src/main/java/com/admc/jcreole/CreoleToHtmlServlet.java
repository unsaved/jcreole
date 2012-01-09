package com.admc.jcreole;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.EnumSet;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import com.admc.util.IOUtil;

public class CreoleToHtmlServlet extends HttpServlet {
    private static Pattern
            servletSubPathPattern = Pattern.compile("(/.+\\.)html");
    private static final String DEFAULT_BP_RES_PATH = "boilerplate-inet.html";

    private String creoleSubdir = "WEB-INF/creole";
    private String rawBoilerPlate = null;
    private String bpResPath = DEFAULT_BP_RES_PATH;

    public void init() throws ServletException {
        super.init();
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
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Matcher matcher = servletSubPathPattern.matcher(req.getServletPath());
        if (!matcher.matches())
            throw new ServletException(
                    "Servlet " + getClass().getName()
                    + " only supports servlet paths ending with '.html':  "
                    + req.getServletPath());
        String creolePath = "/" + creoleSubdir + matcher.group(1) + "creole";
        InputStream iStream =
                getServletContext().getResourceAsStream(creolePath);
        if (iStream == null)
            throw new ServletException("Failed to access:  " + creolePath);

        JCreole jCreole = (rawBoilerPlate == null)
                ? (new JCreole()) : (new JCreole(rawBoilerPlate));

        /*  TODO:  Support interWikiMappers
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
        */

        jCreole.setPageTitle(req.getServletPath());

        // TODO:  Set default of no-raw-html, and support configuration:
        jCreole.setPrivileges(EnumSet.allOf(JCreolePrivilege.class));

        String html = jCreole.postProcess(
                jCreole.parseCreole(IOUtil.toStringBuilder(iStream)), "\n");
        resp.setBufferSize(1024);
        resp.setContentType("text/html");
        resp.getWriter().print(html);
    }
}
