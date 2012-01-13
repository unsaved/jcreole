package com.admc.jcreole;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.EnumSet;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.URL;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import com.admc.util.IOUtil;

public class CreoleToHtmlServlet extends HttpServlet {
    private static Pattern servletFilePattern = Pattern.compile("(.+\\.)html");

    private String creoleSubdir = "WEB-INF/creole";
    EnumSet<JCreolePrivilege> jcreolePrivs =
            EnumSet.complementOf(EnumSet.of(JCreolePrivilege.RAWHTML));

    public void init() throws ServletException {
        super.init();
        /*
         * TODO:  Process parameters, like for creoleSubdir.

        try {
            if (bpResPath.length() > 0 && bpResPath.charAt(0) == '/')
                // Classloader lookups are ALWAYS relative to CLASSPATH roots,
                // and will abort if you specify a beginning "/".
                bpResPath = bpResPath.substring(1);
            InputStream iStream = Thread.currentThread()
                    .getContextClassLoader().getResourceAsStream(bpResPath);
            if (iStream == null)
                throw new IOException("Boilerplate inaccessible: " + bpResPath);
            rawBoilerPlate = IOUtil.toString(iStream);
        } catch (IOException ioe) {
            throw new ServletException("Failed to prepare boilerplate", ioe);
        }
        */
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        File css, creoleDir;
        URL url;
        List<String> cssHrefs = new ArrayList<String>();
        File servletPathFile = new File(req.getServletPath());
        String contextPath = getServletContext().getContextPath();
        InputStream bpStream = null;
        Matcher matcher = servletFilePattern.matcher(servletPathFile.getName());
        if (!matcher.matches())
            throw new ServletException(
                    "Servlet " + getClass().getName()
                    + " only supports servlet paths ending with '.html':  "
                    + servletPathFile.getAbsolutePath());
        File crRootedDir = servletPathFile.getParentFile();
        File creoleFile =
                new File("/" + creoleSubdir + crRootedDir.getAbsolutePath(),
                matcher.group(1) + "creole");
        InputStream creoleStream = getServletContext()
                .getResourceAsStream(creoleFile.getAbsolutePath());
        if (creoleStream == null)
            throw new ServletException(
                    "Failed to access:  " + creoleFile.getAbsolutePath());

        while (crRootedDir != null) {
            creoleDir = new File(
                    "/" + creoleSubdir + crRootedDir.getAbsolutePath());
            if (bpStream == null) {
                bpStream = getServletContext().getResourceAsStream(new File(
                        creoleDir, "boilerplate.html").getAbsolutePath());
            }
            url = getServletContext().getResource(new File(
                    crRootedDir, "site.css").getAbsolutePath());
            if (url != null) cssHrefs.add(0,
                    new File("/" + contextPath + crRootedDir, "site.css")
                    .getAbsolutePath());
            crRootedDir = crRootedDir.getParentFile();
        }
        if (bpStream == null)
            throw new ServletException("Failed to access 'boilerplate.html' "
                    + "from creole dir or ancesotr dir");
        crRootedDir = servletPathFile.getParentFile();
        while (crRootedDir != null) {
            url = getServletContext().getResource(new File(
                    crRootedDir, "jcreole.css").getAbsolutePath());
            if (url != null) cssHrefs.add(0,
                    new File("/" + contextPath + crRootedDir, "jcreole.css")
                    .getAbsolutePath());
            crRootedDir = crRootedDir.getParentFile();
        }

        JCreole jCreole = new JCreole(IOUtil.toString(bpStream));
        if (cssHrefs.size() > 0) jCreole.addCssHrefs(cssHrefs);

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

        jCreole.setPrivileges(jcreolePrivs);

        String html = jCreole.postProcess(
                jCreole.parseCreole(IOUtil.toStringBuilder(creoleStream)), "\n");
        resp.setBufferSize(1024);
        resp.setContentType("text/html");
        resp.getWriter().print(html);
    }
}
