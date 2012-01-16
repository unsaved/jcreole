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
import java.io.FileFilter;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import com.admc.util.IOUtil;
import com.admc.util.Expander;
import com.admc.util.FileComparator;

/**
 * The supplied 'index.creole' file documents usage of this class.
 *
 * That file will be presented (in HTML format) as the home page if you
 * deploy the JCreole-provided war file.
 */
public class CreoleToHtmlServlet
        extends HttpServlet implements InterWikiMapper {
    private static Pattern servletFilePattern = Pattern.compile("(.+)\\.html");

    private String creoleDir = "WEB-INF/creole";
    private boolean autoIndexing = true;

    private String contextPath;
    private ServletContext application;
    private Indexer indexer = new Indexer();
    private static SimpleDateFormat isoDateTimeFormatter =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private static SimpleDateFormat isoDateFormatter =
            new SimpleDateFormat("yyyy-MM-dd");
    private static Pattern sortParamPattern = Pattern.compile("([-+])(\\w+)");

    {
        indexer.setFilter(new FileFilter() {
            public boolean accept(File file) {
                String n = file.getName();
                if (n.length() < 1 || n.charAt(0) == '.') return false;
                if (n.equalsIgnoreCase("web-inf")) return false;
                if (n.equalsIgnoreCase("meta-inf")) return false;
                if (file.isDirectory()) return true;
                if (!n.endsWith(".creole")) return false;
                return !n.equals("index.creole");
            }
        });
        indexer.setNameTranslationMatchPat("(\\w+)\\Q.creole");
        indexer.setNameTranslationFormat("%1$s.html");
    }

    EnumSet<JCreolePrivilege> jcreolePrivs =
            EnumSet.complementOf(EnumSet.of(JCreolePrivilege.RAWHTML));

    public void init() throws ServletException {
        super.init();
        application = getServletContext();
        String creoleDirParam = application.getInitParameter("creoleDir");
        if (creoleDirParam != null) creoleDir = creoleDirParam;
        if (creoleDir == null || creoleDir.length() < 1
                || creoleDir.charAt(0) == '/'
                || creoleDir.charAt(0) == '\\')
            throw new ServletException(
                    "'creoleDir' is not a relative path: " + creoleDirParam);
        String autoString = application.getInitParameter("autoIndexing");
        autoIndexing = autoString == null || Boolean.parseBoolean(autoString);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        File css;
        URL url;
        StringBuilder readmeSb = null;
        File fsCreoleDir = null;
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
        String absUrlDirPath = contextPath + crRootedDir.getAbsolutePath();
        String absUrlBasePath = absUrlDirPath + '/' + pageBaseName;
        File creoleFile =
                new File("/" + creoleDir + crRootedDir.getAbsolutePath(),
                pageBaseName + ".creole");
        InputStream creoleStream = null;
        creoleStream =
                application.getResourceAsStream(creoleFile.getAbsolutePath());
        if (indexer != null) {
            String fsDirPath = application.getRealPath(
                    creoleFile.getParentFile().getAbsolutePath());
            if (fsDirPath == null)
                throw new ServletException(
                    "You must disable indexing with non-explode app servers");
            fsCreoleDir = new File(fsDirPath);
        }
        if (pageBaseName.equals("index")) {
            InputStream readmeStream = application.getResourceAsStream(
                    new File(creoleFile.getParentFile(), "readme.creole")
                    .getAbsolutePath());
            readmeSb = new StringBuilder("----\n");
            if (readmeStream == null) {
                readmeSb.append("{{{\n");
                readmeStream = application.getResourceAsStream(
                        new File(crRootedDir, "readme.txt")
                        .getAbsolutePath());
                if (readmeStream != null) {
                    readmeSb.append(IOUtil.toStringBuilder(readmeStream));
                    readmeSb.append("\n}}}\n");
                }
            } else {
                readmeSb.append(IOUtil.toStringBuilder(readmeStream));
            }
            if (readmeStream == null) readmeSb = null;
        }

        boolean inAncestorDir = false;
        while (crRootedDir != null) {
            File curCreoleDir = new File(
                    "/" + creoleDir + crRootedDir.getAbsolutePath());
            if (bpStream == null)
                bpStream = application.getResourceAsStream(new File(
                        curCreoleDir, "boilerplate.html").getAbsolutePath());
            url = application.getResource(new File(
                    crRootedDir, "site.css").getAbsolutePath());
            if (url != null) cssHrefs.add(0,
                    new File(contextPath + crRootedDir, "site.css")
                    .getAbsolutePath());
            if (creoleStream == null && inAncestorDir
                    && pageBaseName.equals("index") && autoIndexing)
                creoleStream = application.getResourceAsStream(new File(
                        curCreoleDir, "index.creole").getAbsolutePath());
            crRootedDir = crRootedDir.getParentFile();
            inAncestorDir = true;
        }
        if (creoleStream == null)
            throw new ServletException(
                    "Failed to access:  " + creoleFile.getAbsolutePath());
        if (bpStream == null)
            throw new ServletException("Failed to access 'boilerplate.html' "
                    + "from creole dir or ancestor dir");
        crRootedDir = servletPathFile.getParentFile();
        while (crRootedDir != null) {
            url = application.getResource(new File(
                    crRootedDir, "jcreole.css").getAbsolutePath());
            if (url != null) cssHrefs.add(0,
                    new File(contextPath + crRootedDir, "jcreole.css")
                    .getAbsolutePath());
            crRootedDir = crRootedDir.getParentFile();
        }

        JCreole jCreole = new JCreole(IOUtil.toString(bpStream));
        Expander htmlExpander = jCreole.getHtmlExpander();
        Date now = new Date();
        htmlExpander.put(
                "isoDateTime", isoDateTimeFormatter.format(now), false);
        htmlExpander.put("isoDate", isoDateFormatter.format(now), false);
        htmlExpander.put("contextPath", contextPath, false);
        htmlExpander.put("pageBaseName", pageBaseName, false);
        htmlExpander.put("pageDirPath", absUrlDirPath, false);
        htmlExpander.put("pageTitle", absUrlBasePath, false);
        if (readmeSb == null) {
            htmlExpander.put("readmeContent", "");
        } else {
            JCreole readmeJCreole = new JCreole();
            readmeJCreole.setHtmlExpander(htmlExpander);
            readmeJCreole.setInterWikiMapper(this);
            readmeJCreole.setPrivileges(jcreolePrivs);
            htmlExpander.put("readmeContent", readmeJCreole.postProcess(
                    readmeJCreole.parseCreole(readmeSb), "\n"), false);
        }
        if (fsCreoleDir != null) {
            FileComparator.SortBy sortBy = FileComparator.SortBy.NAME;
            boolean ascending = true;
            String sortStr = req.getParameter("sort");
            if (sortStr != null) {
                Matcher m = sortParamPattern.matcher(sortStr);
                if (!m.matches())
                    throw new ServletException(
                            "Malformatted sort value: " + sortStr);
                ascending = m.group(1).equals("+");
                try {
                    sortBy = Enum.valueOf(
                            FileComparator.SortBy.class, m.group(2));
                } catch (Exception e) {
                    throw new ServletException(
                            "Malformatted sort string: " + sortStr);
                }
            }
            htmlExpander.put("index", "\n"
                    + indexer.generateTable(fsCreoleDir, absUrlDirPath, true,
                    sortBy, ascending), false);
        }

        /* Set up Creole macros like this:
        Expander creoleExpander = new Expander();
        creoleExpander.put("testMacro", "\n\n<<prettyPrint>>\n{{{\n"
                + "!/bin/bash -p\n\ncp /etc/inittab /tmp\n}}}\n");
        jCreole.setCreoleExpander(creoleExpander);
        */

        if (cssHrefs.size() > 0) jCreole.addCssHrefs(cssHrefs);
        jCreole.setInterWikiMapper(this);
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
