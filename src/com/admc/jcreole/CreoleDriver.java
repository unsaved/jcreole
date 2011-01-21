package com.admc.jcreole;

import java.io.IOException;
import java.io.File;

public class CreoleDriver {
    public static void main(String[] sa)
            throws IOException, beaver.Parser.Exception {
        if (sa.length != 1)
            throw new IllegalArgumentException(
                    "SYNTAX: java com.admc.play.CreoleDriver file.creole");
        CreoleScanner scanner =
                CreoleScanner.newCreoleScanner(new File(sa[0]), false);
                // Change 'false' to 'true' to silently strip bad input chars.
                // instead of aborting with notification.
        CreoleParser p = new CreoleParser();
        // p.setValidateOnly(true);
        Object retVal = p.parse(scanner);
        System.out.print((retVal == null) ? "<NULL>\n" : ("[" + retVal + ']'));
    }
}
