package com.admc.play;

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
        System.out.println("Parse returns: " + p.parse(scanner));
        System.out.println("========================\n" + p.getOutput());
    }
}
