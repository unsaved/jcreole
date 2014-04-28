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

/**
 * Map from wiki page names to actual paths or URLs to the pages.
 * Implement one of these and use the CreoleParser setter to use it.
 * <p>
 * In order to keep the name short and recognizable, we have decided that what
 * we are calling Inter-wiki links also includes <em>Intra-wiki</em> links.
 * It is up to the implementation to support external wikiNames, null wikiNames
 * (for intra-wiki links), or both.
 * </p>
 *
 * @see CreoleParser#setInterWikiMapper(InterWikiMapper)
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 1.0
 */
public interface InterWikiMapper {
    /**
     * @return the URL path to be written for the link.
     * @param wikiName  if null, then this is an <em>intra-</em>wiki link.
     *                  Must contain a non-lowercase-letter to distinguish a
     *                  wikiName from an URL protocol.
     * @throws CreoleParseException if the given name or page is invalid, you
     *                  can't map them, or you have not implemented the needed
     *                  (intra- vs. inter-) type mapping.
     */
    public String toPath(String wikiName, String wikiPage);

    /**
     * @return the Label to be written for the link.
     * @param wikiName  if null, then this is an <em>intra-</em>wiki link.
     *                  Must contain a non-lowercase-letter to distinguish a
     *                  wikiName from an URL protocol.
     * @throws CreoleParseException if the given name or page is invalid, you
     *                  can't map them, or you have not implemented the needed
     *                  (intra- vs. inter-) type mapping.
     */
    public String toLabel(String wikiName, String wikiPage);
}
