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
 * Runs the Parser.
 *
 * This does not generate entire HTML pages, but just the HTML fragment
 * corresponding to the input Creole Wikitext.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.0
 */
public class CreoleParseDriver {
    /**
     * Run with no parameters to see the syntax banner.
     */
    public static void main(String[] sa)
            throws IOException, beaver.Parser.Exception {
        if (sa.length != 1)
            throw new IllegalArgumentException(
                "SYNTAX: java com.admc.jcreole.CreoleParseDriver file.creole");
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
