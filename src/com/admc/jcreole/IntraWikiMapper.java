package com.admc.jcreole;

/**
 * Map from wiki page names to actual paths or URLs to the pages.
 * Implement one of these and use the CreoleParser setter to use it.
 *
 * @see CreoleParser#setIntraWikiMapper(IntraWikiMapper)
 */
public interface IntraWikiMapper {
    public String toPath(String wikiName, String wikiPage);
}
