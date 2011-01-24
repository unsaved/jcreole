package com.admc.jcreole;

import java.io.IOException;
import java.io.File;

/**
 * Run the Scanner.
 * This is only useful to people who understand scanners, or who can
 * decipher the specifications in the *.flex file.
 * If you do know that, it can be immensely useful for troubleshooting and for
 * adding new capabilities.
 */
public class JFlexDriver {
    /**
     * Run with no parameters to see the syntax banner.
     */
    public static void main(String[] sa)
            throws IOException, CreoleParseException {
        if (sa.length != 1)
            throw new IllegalArgumentException(
                    "SYNTAX: java com.admc.jcreole.JFlexDriver data.file");
        CreoleScanner scanner =
                CreoleScanner.newCreoleScanner(new File(sa[0]), false);
                // Change 'false' to 'true' to silently strip bad input chars.
                // instead of aborting with notification.
        Token token;
        while ((token = scanner.nextToken()).getId() != Terminals.EOF)
            System.out.println(token);
        System.out.println("Exiting gracefully");
    }
}
