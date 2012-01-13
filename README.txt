JCreole is a program that...

    you can use interactively to convert .creole files to .html files
    
    and a library that Java developers can use in any program (standalone,
    client/server, web app,...) to dynamically generate high quality HTML from
    creole text.

Basic usage instructions are at
http://admc.com/projectdocs/jcreole/using.html


Project Status

    RECENT SIGNIFICANT CHANGES  (In latest public release)

        + JCreole authors may use ${references} for variables provided by
          their JCreole product.  The JCreole-provided command-line program
          makes Java system properties available in this way.

        + Implemented Pretty-printing

        + Provide minimal Servlet, CreoleToHtmlServlet, for dynamically storing
         .creole files.  This is a read-only function to serve documentation.
         This first cut is not configurable.

    UPCOMING SIGNIFICANT CHANGES
    (Will be in next public release.  May or may not be available in the trunk
    code base when you read this).

        + Styles encapsulated into .css files to eliminate redundancy and to
          make it more convenient for integrators to use our styles or to
          start with them as a base or template.

        + CreoleToHtmlServlet greatly enhanced.  As the name indicates, it is
          still strictly for serving documentation from .creole files (Creole
          must come from files, not databases, etc., and users can't store or
          modify the Creole.  Integrators could add these features, of course).

        + Will add a HttpRequestHandler as a functional equivalent to
          CreoleToHtlServlet for Spring web apps.

        + Will distribute a .war file that can be dropped into a servlet
          container to serve .creole files that you put under the 'creole'
          directory.
