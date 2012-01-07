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


package com.admc.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedReader;

public class IOUtil {
    /**
     * This class provides only static methods.  Do not instantiate.
     */
    private IOUtil() { }

    /**
     * Wrapper for toString(InputStream, int) with buffer size of
     * 10240 characters.
     *
     * @see #toString(InputStream, int)
     */
    public static String toString(InputStream inputStream) throws IOException {
        return IOUtil.toString(inputStream, 10240);
    }

    /**
     * Generates a String from specified InputStream, using UTF-8 encoding.
     */
    public static String toString(
            InputStream inputStream, int bufferChars) throws IOException {
        return IOUtil.toStringBuilder(inputStream, bufferChars).toString();
    }

    /**
     * Wrapper for toStringBuilder(InputStream, int) with buffer size of
     * 10240 characters.
     *
     * @see #toStringBuilder(InputStream, int)
     */
    public static StringBuilder toStringBuilder(InputStream inputStream)
            throws IOException {
        return IOUtil.toStringBuilder(inputStream, 10240);
    }

    /**
     * Generates a StringBuilder from specified InputStream,
     * using UTF-8 encoding.
     */
    public static StringBuilder toStringBuilder(
            InputStream inputStream, int bufferChars) throws IOException {
        char[] buffer = new char[bufferChars];
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, "UTF-8"));
        int i;
        try {
            StringBuilder sb = new StringBuilder();
            while ((i = reader.read(buffer)) > -1) sb.append(buffer, 0, i);
            return sb;
        } finally {
            try {
                reader.close();
            } catch (IOException ioe) {
                // Don't want any dependency upon logger classes or
                // stdout/stderr, so for now just ignore close failures.
            }
            reader = null;
        }
    }
}
