/*
 * Copyright 2011 Axis Data Management Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.admc.jcreole;

import java.io.IOException;
import java.io.File;

/**
 * Run the Scanner.
 * This is only useful to people who understand scanners, or who can
 * decipher the specifications in the *.flex file.
 * If you do know that, it can be immensely useful for troubleshooting and for
 * adding new capabilities.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.0
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
                CreoleScanner.newCreoleScanner(new File(sa[0]), false, null);
                // Change 'false' to 'true' to silently strip bad input chars.
                // instead of aborting with notification.
        Token token;
        while ((token = scanner.nextToken()).getId() != Terminals.EOF)
            System.out.println(token);
        System.out.println("Exiting gracefully");
    }
}
