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

import java.util.List;
import java.util.ArrayList;

/**
 * A list of sections that can be displayed in a Table of Contents
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class Sections extends ArrayList<SectionHeading> {
    /**
     * @param levelInclusions  char array of length 6.  Each char is just
     *        checked for 'x' to indicate to skip that level in the TOC.
     *        If the char for an index is not 'x', then a record for that
     *        heading/section will be written according to the SectionHeading
     *        record.
     */
    public String generateToc(String levelInclusions) {
        if (levelInclusions == null)
            throw new NullPointerException("levelInclusions may not be null");
        if (levelInclusions.length() != 6)
            throw new IllegalArgumentException(
                    "levelInclusions has length " + levelInclusions.length()
                    + " instead of 6:  " + levelInclusions);

        String ulCloser;
        List<String> unravelStack = new ArrayList<String>();
        // menuLevels is 0-based just like levelInclusions.
        int[] menuLevels = new int[6];
        int nextLevel = 0;
        for (int i = 0; i < levelInclusions.length(); i++)
            menuLevels[i] = (levelInclusions.charAt(i) == 'x')
                          ? -1 : nextLevel++;
        int menuLevel = -1, newMenuLevel;
        String seqLabel;
        StringBuilder sb = new StringBuilder();
        for (SectionHeading sh : this) {
            // sh level is 1 more than array indexes
            newMenuLevel = menuLevels[sh.getLevel() - 1];
            if (newMenuLevel < 0) continue;  // Don't display this level

            if (newMenuLevel == menuLevel) {
                sb.append("</li>\n");
            } else if (newMenuLevel > menuLevel) {
                for (int i = menuLevel + 1; i <= newMenuLevel; i++) {
                    sb.append((i == 0)
                            ? "<ul class=\"jcx_toc\">\n"
                            : (((i == menuLevel + 1)
                                ? "" : CreoleParser.indent(i))
                              + "<ul>\n"));
                    ulCloser = "\n" + CreoleParser.indent(i) + "</ul>";
                    unravelStack.add(0, (i == newMenuLevel)
                            ? ("</li>" + ulCloser) : ulCloser);
                            // When back out, do need to close a li?
                }
            } else if (newMenuLevel < menuLevel) {
                for (int i = menuLevel; i > newMenuLevel; i--)
                    sb.append(unravelStack.remove(0));
                sb.append("</li>\n");
            }
            menuLevel = newMenuLevel;
            sb.append(CreoleParser.indent(menuLevel+1))
                    .append("<li><a href=\"#")
                    .append(sh.getXmlId()).append("\" target=\"_self\">");
            seqLabel = sh.getSequenceLabel();
            if (seqLabel != null) {
                sb.append("<span class=\"jcx_enum\">")
                .append(seqLabel).append("</span> ");
            }
            sb.append(sh.getText()).append("</a>");
        }
        while (unravelStack.size() > 0) sb.append(unravelStack.remove(0));
        return sb.toString();
    }
}
