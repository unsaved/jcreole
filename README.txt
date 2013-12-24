JCreole is a program that...

    you can use interactively to convert .creole files to .html files

    you can use to present .creole files from your JEE application server
    
    and a library that Java developers can use in any program (standalone,
    client/server, web app,...) to dynamically generate high quality HTML from
    creole text.

Please visit http://admc.com/projectdocs/jcreole/using.html for download links and basic usage instructions.

In order to maximize portability, we require Java 6 code, so in order to
compile you will need a Java 6 JRE jar file.  When you attempt your first
compilation, Gradle will tell you to write the file path for it in a properties
file.  You only need the Java 6 JRE jar FILE.  Your main JDK can be later.


Project Status

    RECENT SIGNIFICANT CHANGES  (In latest public release)

        + JCreole authors may use ${references} for variables provided by
          their JCreole product.  The JCreole-provided command-line program
          makes Java system properties available in this way.

        + Implemented Pretty-printing

        + Provide web applications docServer*.war and sprinDocServer*.war.
          to dynamically present .creole files that you plunk onto your
          application server.  Just rename to desired web appplication name
          (+ ".war") and deploy it.  Drill down into "docServer.creole" from
          the home page to find out exactly where to place your *.creole
          files, and how to customize the configuration if desired.

        + The classes CreoleToHtmlServlet and CreoleToHtmlHandler, used by
          the web applications of the previous item, can be used in your own
          web applications.

        + Styles encapsulated into .css files to eliminate redundancy and to
          make it more convenient for integrators to use our styles or to
          start with them as a base or template.
