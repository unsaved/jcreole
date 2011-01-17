package com.admc.play;

import java.io.IOException;
import java.io.FileInputStream;

public class FlexDriver {
    public static void main(String[] sa) throws IOException {
        if (sa.length != 1)
            throw new IllegalArgumentException(
                    "SYNTAX: java com.admc.play.FlexDriver data.file");
        Scanner scanner = new Scanner(new FileInputStream(sa[0]));
        Token token;
        while ((token = scanner.nextToken()).getId() != Terminals.EOF)
            System.out.println(token);
        System.out.println("Exiting gracefully");
    }
}
