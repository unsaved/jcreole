package com.admc.jcreole;

import java.util.List;
import java.io.IOException;
import java.io.FileInputStream;

public class CreoleDriver {
    public static void main(String[] sa) throws Exception {
        if (sa.length != 1)
            throw new IllegalArgumentException(
                    "SYNTAX: java com.admc.play.CreoleDriver file.creole");
        CreoleScanner scanner = new CreoleScanner(new FileInputStream(sa[0]));
        CreoleParser p = new CreoleParser();
        // p.setValidateOnly(true);
        Object retVal = p.parse(scanner);
        System.out.print((retVal == null) ? "<NULL>\n" : retVal);
    }
}
