package com.admc.jcreole;

import java.io.IOException;
import java.io.File;

public class FlexDriver {
    public static void main(String[] sa)
            throws IOException, CreoleParseException {
        if (sa.length != 1)
            throw new IllegalArgumentException(
                    "SYNTAX: java com.admc.play.FlexDriver data.file");
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
