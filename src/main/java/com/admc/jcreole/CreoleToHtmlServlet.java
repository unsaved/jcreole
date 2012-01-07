package com.admc.jcreole;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import com.admc.util.IOUtil;

public class CreoleToHtmlServlet extends HttpServlet {
    static Pattern servletSubPathPattern = Pattern.compile("(/.+\\.)html");

    private String creoleSubdir = "WEB-INF/creole";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Matcher matcher = servletSubPathPattern.matcher(req.getServletPath());
        if (!matcher.matches())
            throw new ServletException(
                    "Servlet " + getClass().getName()
                    + " only supports servlet paths ending with '.html':  "
                    + req.getServletPath());
        String creolePath = "/" + creoleSubdir + matcher.group(1) + "txt";
        InputStream iStream =
                getServletContext().getResourceAsStream(creolePath);
        if (iStream == null)
            throw new ServletException("Failed to access:  " + creolePath);
        resp.setBufferSize(1024);
        resp.setContentType("text/html");
        resp.getWriter().print(IOUtil.toString(iStream));
    }
}
