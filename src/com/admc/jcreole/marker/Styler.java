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


package com.admc.jcreole.marker;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.admc.jcreole.CreoleParseException;

/**
 * Adds CSS class to HTML tags in the output buffer.
 *
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.1
 */
public class Styler extends BufferMarker {
    private static final Pattern CssNamePattern =
            Pattern.compile("[a-z][a-zA-Z_][-\\w]*");
    enum TargetDirection { PREVIOUS, CONTAINER, NEXT }

    protected String className;
    protected TargetDirection targetDirection;

    public Styler(int id, String newClassName, TargetDirection direction) {
        super(id);
        if (newClassName == null)
            throw new NullPointerException(
                    "Styler specifies no CSS class name");
        Matcher m = CssNamePattern.matcher(newClassName);
        if (!m.matches())
            throw new CreoleParseException(
                    "Illegal class name: " + newClassName);
        this.className = newClassName;
        this.targetDirection = direction;
    }
}
