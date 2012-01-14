package com.admc.jcreole;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.EnumSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import com.admc.util.IOUtil;
import com.admc.util.Expander;

/**
 * The supplied 'index.creole' file documents usage of this class.
 *
 * That file will be presented (in HTML format) as the home page if you
 * deploy the JCreole-provided war file.
 */
public class CreoleToHtmlServlet
        extends HttpServlet implements InterWikiMapper {
    private static Pattern servletFilePattern = Pattern.compile("(.+)\\.html");
    private String contextPath;
    private ServletContext application;

    private String creoleSubdir = "WEB-INF/creole";
    EnumSet<JCreolePrivilege> jcreolePrivs =
            EnumSet.complementOf(EnumSet.of(JCreolePrivilege.RAWHTML));

    public void init() throws ServletException {
        super.init();
        application = getServletContext();
        String creoleSubdirParam = application.getInitParameter("creoleSubdir");
        if (creoleSubdirParam != null) creoleSubdir = creoleSubdirParam;
        if (creoleSubdir == null || creoleSubdir.length() < 1
                || creoleSubdir.charAt(0) == '/'
                || creoleSubdir.charAt(0) == '\\')
            throw new ServletException(
                    "'creoleSubdir' is not a relative path: " + creoleSubdir);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        File css, creoleDir;
        URL url;
        List<String> cssHrefs = new ArrayList<String>();
        File servletPathFile = new File(req.getServletPath());
        if (contextPath == null) {
            contextPath = application.getContextPath();
            iwUrls.put("home", contextPath);
            String appName = application.getServletContextName();
            iwLabels.put("home",
                    ((appName == null) ? "Site" : appName) + " Home Page");
        }
        InputStream bpStream = null;
        Matcher matcher = servletFilePattern.matcher(servletPathFile.getName());
        if (!matcher.matches())
            throw new ServletException(
                    "Servlet " + getClass().getName()
                    + " only supports servlet paths ending with '.html':  "
                    + servletPathFile.getAbsolutePath());
        File crRootedDir = servletPathFile.getParentFile();
        String pageBaseName = matcher.group(1);
        File creoleFile =
                new File("/" + creoleSubdir + crRootedDir.getAbsolutePath(),
                pageBaseName + ".creole");
        InputStream creoleStream = application
                .getResourceAsStream(creoleFile.getAbsolutePath());
        if (creoleStream == null)
            throw new ServletException(
                    "Failed to access:  " + creoleFile.getAbsolutePath());

        while (crRootedDir != null) {
            creoleDir = new File(
                    "/" + creoleSubdir + crRootedDir.getAbsolutePath());
            if (bpStream == null) {
                bpStream = application.getResourceAsStream(new File(
                        creoleDir, "boilerplate.html").getAbsolutePath());
            }
            url = application.getResource(new File(
                    crRootedDir, "site.css").getAbsolutePath());
            if (url != null) cssHrefs.add(0,
                    new File(contextPath + crRootedDir, "site.css")
                    .getAbsolutePath());
            crRootedDir = crRootedDir.getParentFile();
        }
        if (bpStream == null)
            throw new ServletException("Failed to access 'boilerplate.html' "
                    + "from creole dir or ancesotr dir");
        crRootedDir = servletPathFile.getParentFile();
        while (crRootedDir != null) {
            url = application.getResource(new File(
                    crRootedDir, "jcreole.css").getAbsolutePath());
            if (url != null) cssHrefs.add(0,
                    new File(contextPath + crRootedDir, "jcreole.css")
                    .getAbsolutePath());
            crRootedDir = crRootedDir.getParentFile();
        }

        Expander creoleExpander = new Expander();
        creoleExpander.put("isoDateTime",
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .format(new Date()), false);
        creoleExpander.put("contextPath", contextPath, false);
        creoleExpander.put("pageBaseName", pageBaseName, false);
        JCreole jCreole = new JCreole(IOUtil.toString(bpStream));
        jCreole.getBpExpander().put("contextPath", contextPath, false);
        if (cssHrefs.size() > 0) jCreole.addCssHrefs(cssHrefs);
        jCreole.setExpander(creoleExpander);
        jCreole.setInterWikiMapper(this);

        jCreole.setPageTitle(req.getServletPath());

        jCreole.setPrivileges(jcreolePrivs);

        String html = jCreole.postProcess(
                jCreole.parseCreole(IOUtil.toStringBuilder(creoleStream)), "\n");
        resp.setBufferSize(1024);
        resp.setContentType("text/html");
        resp.getWriter().print(html);
    }

    // InterWikiMapper implementation follows
    protected Map<String, String> iwUrls = new HashMap<String, String>();
    protected Map<String, String> iwLabels = new HashMap<String, String>();

    // TODO:  Add translations for all of the popular public Wikis
    public String toPath(String wikiName, String wikiPage) {
        if (wikiPage == null)
            throw new RuntimeException(
                    "wiki page name not given to InterWikiMapper");
        if (wikiName == null) return iwUrls.get(wikiPage);
        if (wikiName.equals("wikipedia"))
            return "http://en.wikipedia.org/wiki/" + wikiPage;
        return null;
    }
    public String toLabel(String wikiName, String wikiPage) {
        if (wikiPage == null)
            throw new RuntimeException(
                    "wiki page name not given to InterWikiMapper");
        if (wikiName == null) return iwLabels.get(wikiPage);
        if (wikiName.equals("wikipedia")) return wikiPage + " @ Wikipedia";
        return null;
    }
}
