Using JCreole

The easiest way to use JCreole is to get a binary distribution (a file of
the format jcreole-*.zip), extract it, and execute the jcreole-*.jar file
therein from your command line.
It will tell you that it wants you give it a parameter telling where the input
file of Creole Wikitext is.  By default it will display the generated HTML
(which you can redirect to a file since it is written to standard output).
The syntax message also shows you can directly tell the program to write an
HTML file.  The most basic usage is something like

    java path/to/jcreole-*.jar path/to/your/file.creole

on UNIX, or

    java path\to\jcreole-*.jar path/to/your/file.creole

on Windows.


To integrate with your own product, see the JavaDocs which are distributed
with both the source and binary distributions.  If you don't plan on
extending the language itself, then stick with the binary distribution and
work from the API spec.  If you want to poke around the source code or take
a look at the Ivy or Ant setup to get ideas, you can view the source code
online or download the source distribution.

Obviously, if you want to change JCreole itself, for example to add new
plugin features, you will need to work with source code.  Your choice whether
to pull from the source code repository or extract from a JCreole source 
distribution.  The Ant file has lots of targets that should be very useful for
integrators.
