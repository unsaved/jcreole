Using JCreole


You can try out JCreole without downloading it, at http://admc.com/jcreole .


GENERATING HTML FROM THE COMMAND-LINE

The easiest way to use JCreole is to get a binary distribution (a file of
the format jcreole-*.zip), extract it, and execute the jcreole-*.jar file
therein from your command line.
It will tell you that it wants you give it a parameter telling where the input
file of Creole Wikitext is.  By default it will display the generated HTML
(which you can redirect to a file since it is written to standard output).
The syntax message also shows you can directly tell the program to write an
HTML file.  The most basic usage is like

    java path/to/jcreole-*.jar path/to/your/file.creole

on UNIX, or

    java path\to\jcreole-*.jar path/to/your/file.creole

on Windows.

If you want to generate just the HTML corresponding to the input Creole,
without wrapping HTML to make it a complete HTML page, then add a "-" switch
before the file name like

    java path/to/jcreole-*.jar - path/to/your/file.creole

on UNIX, or

    java path\to\jcreole-*.jar - path/to/your/file.creole


To integrate with your own product, see the JavaDocs which are distributed
with both the source and binary distributions (or at
http://admc.com/jcreole/apidocs/ ).  If you don't plan on extending the language
itself, then stick with the binary distribution and work from the API spec,
except to definitely look at the source code for method
com.admc.jcreole.JCreole.main().
It's likely that you can copy and adjust working code from JCreole.main() to do
exactly what you want to do.
Even if you need to use CreoleScanner and CreoleParser instances directly, you
can learn what you need to do by studying JCreole.main() and working down.
You can view the source code online or download the source distribution.

Obviously, if you want to change JCreole itself, for example to add new
plugin features, you will need to work with source code.  Your choice whether
to pull from the source code repository or extract from a JCreole source 
distribution.  The Ant file has lots of targets that should be very useful for
integrators.  As every Ant user should know, run "ant -p" from the main
directory to list concise descriptions of the available targets.


LICENSE

JCreole is copyrighted with an Apache 2.0 license by Axis Data Management Corp.
See the "LICENSE.txt" file in the "doc" directory of all distributions.


DOCUMENTATION

There are text documentation files in the doc subdirectory.

There is also a reference guide for Creole authors.
It is provided in 100% standalone HTML format in the binary zip distribution as
"jcreole-ref.html" in the doc directory.  But the source Creole
for it is also provided as "jcreole-ref.creole" in the jcreole-*.jar file and
in the "resources" source code directory.  Integrators can use
"jcreole-ref.creole" to serve the author guide framed and styled like your other
application content.  You can obviously edit "jcreole-ref.creole".  The Ant
target "ref" will build "tmp/jcreole-ref.html" from the .creole file.
For a more interactive reference that is not 100% standalone, set Ant property
"refstyle" to "jqueryui" before executing the "ref" target.
(The jqueryui-mode document is standalone in that it will work fine if you
distribute that one file, but it requires an Internet connection at read-time
because of references to resources at https://ajax.googleapis.com.

The web site http://admc.com/jcreole has interactive JCreole demonstrations and
comprehensive Creole tutorials.
