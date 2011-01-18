package com.admc.play;

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
        System.out.println("Parse returns: " + retVal);
        System.out.println("========================\n" + p.getOutput());
        System.out.println("------------------------\n");
        if (retVal != null) {
            System.out.println("retval isa = " + retVal.getClass().getName());
            int i = 0;
            if (retVal instanceof List) for (Object o : (List) retVal)
                if (o instanceof beaver.Symbol)
                    System.out.println("  S" + (++i) + ": ("
                            + (((beaver.Symbol) o).value)
                            + "), a " + ((beaver.Symbol) o).value.getClass().getName());
                else
                    System.out.println("  #" + (++i) + ": (" + o
                            + "), a " + o.getClass().getName());
        }
    }
}
