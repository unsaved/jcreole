package com.admc.play;

import java.io.IOException;
import java.io.FileInputStream;

public class BeaverDriver {
    public static void main(String[] sa) throws Exception {
        if (sa.length != 1)
            throw new IllegalArgumentException(
                    "SYNTAX: java com.admc.play.BeaverDriver data.file");
        Scanner scanner = new Scanner(new FileInputStream(sa[0]));
        BeavParser p = new BeavParser();
        // p.setValidateOnly(true);
        System.out.println("Parse returns: " + p.parse(scanner));
        System.out.println("========================\n" + p.getOutput());
    }
}
