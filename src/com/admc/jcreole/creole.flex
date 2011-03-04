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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CharSequenceReader;

%%
%class CreoleScanner
%public
%unicode
%eofclose
%line
%column
%char
%extends beaver.Scanner
%function nextToken
%type Token
%yylexthrow CreoleParseException

%{
    private static final Pattern BlockPrePattern =
            Pattern.compile("(?s)\\Q{{{\\E\n(.*?)\n\\Q}}}\\E\n");
    private static final Pattern InlinePrePattern =
            Pattern.compile("(?s)\\Q{{{\\E(.*?)\\Q}}}");
    private static final Pattern ListLevelPattern =
            Pattern.compile("\\s*([#*]+)(=?)");
    private static final Pattern ParamPluginPattern =
            Pattern.compile("(?s)<<\\s*(\\w+)\\s+(.*\\S)\\s*>>");
    private static final Pattern OptParamPluginPattern =
            Pattern.compile("(?s)<<\\s*(\\w+)(?:\\s+(.*\\S))?\\s*>>");
    private static final Pattern JcxPattern =
            Pattern.compile("(?s)<<\\s*([\\[{])\\s*(.*\\S)?\\s*>>");
    private static final Pattern IndexSandwichPattern = Pattern.compile(
            "(?s)(<<\\s*\\(\\s*>>\\s*)(.*?\\S)\\s*<<\\s*\\)\\s*>>");

    private boolean needIndexCloser;
    private Token newToken(short id) {
        return new Token(id, null, yychar, yyline, yycolumn);
    }
    private Token newToken(short id, String s) {
        return new Token(id, s, yychar, yyline, yycolumn);
    }
    private Token newToken(short id, String s, int intParam) {
        return new Token(id, s, yychar, yyline, yycolumn, intParam);
    }

    private int urlDeferringState, listLevel;
    private List<Integer> stateStack = new ArrayList<Integer>();

    public List<Integer> getStateStack() { return stateStack; }

    private void pushState() {
        stateStack.add(0, yystate());
    }
    private int popState() { return stateStack.remove(0); }

    /**
     * Static factory method.
     *
     * @param doClean If true will silently remove illegal input characters.
     *                If false, will throw if encounter any illegal input char.
     */
    public static CreoleScanner newCreoleScanner(
            File inFile, boolean doClean) throws IOException {
        return newCreoleScanner(
                new StringBuilder(FileUtils.readFileToString(inFile, "UTF-8")),
                doClean);
    }

    /**
     * Static factory method.
     * This method will always silently filter out \r's.
     * The doClean parameter says what to do about control characters other
     * than \r (silently filtered) and \n and tabs (allowed and retained).
     *
     * @param sb StringBuilder containing any characters that we will filter
     *           and/or validate.
     * @param doClean If true will silently remove illegal input characters.
     *                If false, will throw if encounter any illegal input char.
     * @throws IllegalArgumentException if doClean is set to false and
     *         control character(s) other than \n, \r, \t are found in the
     *         StringBuilder.
     */
    public static CreoleScanner newCreoleScanner(
            StringBuilder sb, boolean doClean) throws IOException {
        List<Integer> badIndexes = new ArrayList<Integer>();
        char c;
        for (int i = sb.length() - 1; i >= 0; i--) {
            c = sb.charAt(i);
            switch (c) {
              case '\r':
                sb.deleteCharAt(i);
              case '\n':
              case '\t':
                continue;
            }
            if (!Character.isISOControl(c)) continue;
            if (doClean) sb.deleteCharAt(i);
            else badIndexes.add(badIndexes.size(), Integer.valueOf(i));
        }
        if (badIndexes.size() > 0)
            throw new IllegalArgumentException(
                    "Illegal input char(s) at following positions: "
                    + badIndexes);
        //if (sb.length() > 0 && sb.charAt(sb.length()-1) != '\n')
            //sb.append('\n');
        return new CreoleScanner(new CharSequenceReader(sb));
    }

    private Matcher matcher(Pattern p) {
        return matcher(p, false);
    }

    private Matcher matcher(Pattern p, boolean doTrim) {
        Matcher m = p.matcher(doTrim ? yytext().trim() : yytext());
        if (!m.matches())
            throw new CreoleParseException(String.format(
                "Creole directive markup text doesn't match expected pattern: "
                + "\"%s\"", yytext()), yychar, yyline, yycolumn);
        return m;
    }
%}

%states PSTATE, LISTATE, ESCURL, TABLESTATE, HEADSTATE, JCXBLOCKSTATE, DLSTATE

S = [^ \t\f\n]
s = [ \t\f\n]
//w = [a-zA-Z0-9_]
wsdash = [- a-zA-Z0-9_]
//wdash = [-a-zA-Z0-9_]
NONPUNC = [^ \t\f\n,.?!:;\"']  // Allowed last character of URLs.  Also non-WS.
%%

// GOTCHA!
// ** Be careful of effects of pushback on ^.
// ** If you don't push back all characters after the line-break, you will
// ** lose the ability to match ^.

// ********************************************************
// State changes from YYINITIAL.
// URLs and a couple special cases are handled elsewhere.
<YYINITIAL> ^[ \t]+ / "**" {
    pushState();
    yybegin(PSTATE);
    return newToken(Terminals.TEXT, yytext());
}
// Gobble up leading whitespace:
<YYINITIAL> ^[ \t]+ / "<<"{s}*"["{wsdash}*">>" { }
<YYINITIAL> ^[ \t]*[#*]"]" {
    pushState();
    yybegin(LISTATE);
    return newToken(Terminals.TAB);
}
<YYINITIAL> ^[ \t]*[#]= {
    pushState();
    yybegin(LISTATE);
    return newToken(Terminals.LI, "#", -1);
}
<YYINITIAL> ^[ \t]*[*]= {
    pushState();
    yybegin(LISTATE);
    return newToken(Terminals.LI, "*", -1);
}
<YYINITIAL> ^[ \t]*; {
    pushState();
    yybegin(DLSTATE);
    return newToken(Terminals.DT);
}
<YYINITIAL> ^[ \t]*[#] / [^#] {
    pushState();
    yybegin(LISTATE);
    return newToken(Terminals.LI, "#", 1);
}
<YYINITIAL> ^[ \t]*[*] / [^*] {
    pushState();
    yybegin(LISTATE);
    return newToken(Terminals.LI, "*", 1);
}
<YYINITIAL, JCXBLOCKSTATE, LISTATE, TABLESTATE, DLSTATE>
"<<"{s}*"["{wsdash}*">>" {
    Matcher m = matcher(JcxPattern);
    if (m.groupCount() != 2)
        throw new RuntimeException(
                "JCX Matcher captured " + m.groupCount() + " groups");
    String classNames = m.group(2);
    // Note the recursion for JCXBLOCKSTATE
    pushState();
    yybegin(JCXBLOCKSTATE);
    return newToken(Terminals.JCXBLOCK,
            (classNames == null) ? null : classNames.replaceAll("\\s+", " "));
}
<YYINITIAL> ^[ \t]*=+ {
    // No need to push or pop for HEADSTATE.  Will revert to YYINITIAL.
    // If you wnat a HEADING from inside a jcxBlock, exit the jcxBlock first.
    yypushback(yylength());
    yybegin(HEADSTATE);
}
<YYINITIAL> ^[ \t]*[|] {
    yypushback(yylength());
    pushState();
    yybegin(TABLESTATE);
}
<YYINITIAL> ^[ \t]* "<<"{s}*[{(] {
    yypushback(yylength());
    pushState();
    yybegin(PSTATE);
}
<YYINITIAL> ^[ \t]* "<<"{s}*(footNote|indexed)[ \t] {
    yypushback(yylength());
    pushState();
    yybegin(PSTATE);
}
<YYINITIAL> "//" {
    pushState();
    yybegin(PSTATE);
    return newToken(Terminals.EM_TOGGLE);
}
<YYINITIAL> ## {
    pushState();
    yybegin(PSTATE);
    return newToken(Terminals.MONO_TOGGLE);
}
<YYINITIAL> -- {
    pushState();
    yybegin(PSTATE);
    return newToken(Terminals.STRIKE_TOGGLE);
}
<YYINITIAL> ---- {
    pushState();
    yypushback(2);
    yybegin(PSTATE);
    return newToken(Terminals.STRIKE_TOGGLE);
}
<YYINITIAL> __ {
    pushState();
    yybegin(PSTATE);
    return newToken(Terminals.UNDER_TOGGLE);
}
<YYINITIAL> "^^" {
    pushState();
    yybegin(PSTATE);
    return newToken(Terminals.SUP_TOGGLE);
}
<YYINITIAL> ,, {
    pushState();
    yybegin(PSTATE);
    return newToken(Terminals.SUB_TOGGLE);
}
<YYINITIAL> "**" {
    pushState();
    yybegin(PSTATE);
    return newToken(Terminals.STRONG_TOGGLE);
}
<YYINITIAL> . {
    pushState();
    yybegin(PSTATE);
    return newToken(Terminals.TEXT, yytext());
}
<YYINITIAL> "<<"{s}*[{}()] {
    pushState();
    yypushback(yylength());
    yybegin(PSTATE);
}
<YYINITIAL> "<<"{s}*(footNoteEntry|masterDefEntry){s} {
    pushState();
    yypushback(yylength());
    yybegin(PSTATE);
}
<YYINITIAL> "<<"{s}*addClass[ \t] {
    // Undesirable situation here.
    // We don't know whether the Styler directive at root level is intended for
    // a para, list, table, or jcxBlock.
    // Until figure out how to determine, assume para, which may cause
    // unnecessary switching back and forth to paras.
    // Probably not worth fixing, since the page author can always avoid this
    // situation by not using <<addClass +jcxBlock...>> at the root level.
    // I.e. if the author wants to style a root-level jcxBlock, then use a
    // <<addClass =jcxBlock...>> instead.
    pushState();
    yypushback(yylength());
    yybegin(PSTATE);
}
// ********************************************************

<JCXBLOCKSTATE> "<<"{s}*"]"{s}*">>" {
    yybegin(popState());
    return newToken(Terminals.END_JCXBLOCK);
}
<LISTATE> ^[ \t]*(#|"*")"]" { return newToken(Terminals.TAB); }
<LISTATE> ^[ \t]*((#+)|("*"+))=? {
    Matcher m = matcher(ListLevelPattern);
    if (m.groupCount() != 2)
        throw new RuntimeException(
                "List-item Matcher captured " + m.groupCount() + " groups");
    return newToken(Terminals.LI,
            Character.toString(m.group(1).charAt(0)),
            m.group(1).length()
            * ((m.group(2) != null && m.group(2).length() > 0) ? -1 : 1)
            );
}
<DLSTATE> ^[ \t]*; { return newToken(Terminals.DT); }
<PSTATE> ^[ \t]*[#*] {
    yypushback(yylength());
    yybegin(LISTATE);
    return newToken(Terminals.END_PARA);
}
<PSTATE> ^[ \t]*; {
    yypushback(yylength());
    yybegin(DLSTATE);
    return newToken(Terminals.END_PARA);
}
<PSTATE> ^[ \t]*[|] {
    yypushback(yylength());
    yybegin(TABLESTATE);
    return newToken(Terminals.END_PARA);
}
<PSTATE> ^([ \t]*\n)+ {
    yybegin(popState());
    yypushback(yylength());
    return newToken(Terminals.END_PARA, "\n");
}
<PSTATE> ^[ \t]*"<<"{s}*(toc|footNotes|masterDefList|index)[ \t>] {
    yybegin(popState());
    yypushback(yylength());
    return newToken(Terminals.END_PARA, "\n");
}
<TABLESTATE> "~"\n { return newToken(Terminals.TEXT, "\n"); } // Escape newline
<TABLESTATE> ("|"[ \t]*) / \n { }  // Strip off optional trailing |.
<TABLESTATE> \n / [ \t]*[^|] {
    yybegin(popState());
    return newToken(Terminals.FINAL_ROW);
}
<TABLESTATE> \n / [ \t]*[|] {
    return newToken(Terminals.END_ROW, null, listLevel);
}
<LISTATE> \n / [ \t]*\n {
    yybegin(popState());
    return newToken(Terminals.FINAL_LI);
}
<LISTATE> \n / [ \t]*"|" {
    yybegin(popState());
    return newToken(Terminals.FINAL_LI);
}
<LISTATE> ^[ \t]*"<<"{s}*(toc|footNotes|masterDefList|index)[ \t>] {
    yybegin(popState());
    yypushback(yylength());
    return newToken(Terminals.FINAL_LI);
}
<LISTATE> \n / [ \t]*[#*] {
    return newToken(Terminals.END_LI, null, listLevel);
}
<DLSTATE> ^[ \t]*[#*] {
    yypushback(yylength());
    yybegin(LISTATE);
    return newToken(Terminals.FINAL_DT);
}
<DLSTATE> \n / [ \t]*\n {
    yybegin(popState());
    return newToken(Terminals.FINAL_DT);
}
<DLSTATE> \n / [ \t]*"|" {
    yybegin(popState());
    return newToken(Terminals.FINAL_DT);
}
<DLSTATE> ^[ \t]*"<<"{s}*(toc|footNotes|masterDefList|index)[ \t>] {
    yybegin(popState());
    yypushback(yylength());
    return newToken(Terminals.FINAL_DT);
}
<DLSTATE> \n / [ \t]*; {
    return newToken(Terminals.END_DT);
}
<TABLESTATE> ^[ \t]*"|=" { return newToken(Terminals.CELL, null, 1); }
  // 1 is the SOH character code for "Start Of Header"
<TABLESTATE> ^[ \t]*"|" { return newToken(Terminals.CELL); }

<YYINITIAL> ^[ \t]*"<<"{s}*"!" ~ ">>" {
    // HTML comments starting at ^[ \t] inside jcxBlocks handled by NESTED_...
    int startIndex = yytext().indexOf('!');
    return newToken(Terminals.ROOTLVL_HTMLCOMMENT,
            yytext().substring(startIndex+1, yylength() - 2));
}
"<<"{s}*"!" ~ ">>" {
    int startIndex = yytext().indexOf('!');
    return newToken(Terminals.NESTED_HTMLCOMMENT,
            yytext().substring(startIndex+1, yylength() - 2));
}
<YYINITIAL, JCXBLOCKSTATE>
^[ \t]*"<<"{s}*(toc|footNotes|masterDefList|index)[ \t>] ~ \n {
    Matcher m = matcher(OptParamPluginPattern, true);
    if (m.groupCount() != 2)
        throw new RuntimeException(
                "JCX Matcher captured " + m.groupCount() + " groups");
    short t;
    if (m.group(1).equals("toc"))
        t = Terminals.TOC;
    else if (m.group(1).equals("footNotes"))
        t = Terminals.FOOTNOTES;
    else if (m.group(1).equals("masterDefList"))
        t = Terminals.MASTERDEFLIST;
    else if (m.group(1).equals("index"))
        t = Terminals.INDEX;
    else throw new IllegalStateException(
            "Unexpected Plugin directive: " + m.group(1));
    return newToken(t, m.group(2));
}
<YYINITIAL> ^[ \t]*"<<"{s}*"~" ~ ">>" {
    // Raw HTML starting at ^[ \t] inside jcxBlocks handled by NESTED_...
    int startIndex = yytext().indexOf('~');
    return newToken(Terminals.ROOTLVL_RAWHTML,
            yytext().substring(startIndex+1, yylength() - 2));
}
"<<"{s}*"~" ~ ">>" {
    int startIndex = yytext().indexOf('~');
    return newToken(Terminals.NESTED_RAWHTML,
            yytext().substring(startIndex+1, yylength() - 2));
}
<YYINITIAL> ^("{{{"\n) ~ (\n"}}}"\n) {
    // Pres starting at ^[ \t] inside jcxBlocks handled by NESTED_...
    return newToken(Terminals.ROOTLVL_PRE, matcher(BlockPrePattern).group(1));
}
"{{{" ~ ("}"* "}}}") {
    if (yystate() == YYINITIAL) {
        pushState();
        yybegin(PSTATE);
    }
    return newToken(Terminals.NESTED_PRE, matcher(InlinePrePattern).group(1));
}

// ~ escapes according to http://www.wikicreole.org/wiki/EscapeCharacterProposal
// plus to change space into nbsp and to escape table row breaks according to
// http://www.wikicreole.org/wiki/HardSpace and
// http://www.wikicreole.org/wiki/Newlines .
// The only variation of my own here is that I do not escape line breaks for
// list items since I believe the premiss of the justification for that is
// wrong:  List items to not end at a line break.
// Also, no need to escape the deprecated "-" at beginning of line.
// ... AND... the author of that table isn't very good with logic--
//    The listing for "~<space>" (which I don't honor) and for "Within Links"
//    should not be in the table at all since they are instances of the default
//    non-escaping behavior (the list says it's a list of what would "trigger
//    escaping, whereas these 2 items are there to say they do not trigger
//    escaping").
// I see no reason at all fo do anything special for ~ inside of URLs.
// None of the cases for escape here will occur in a real-world legal URL.
// Ah, I see that they missed ^"*".
// AND! I see that though they missed inline {{{}}}, they have written the
// entirely redundant listing for "Nowiki Open/Close (handled by the
// Image Open/Close in a more general way).
// ANYWHERES
"~*"[*]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~/"[/]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~#"[#]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~-"[-]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~_"[_]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~^"[\^]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~,"[,]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~["[\[]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~]"[\]]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~\\"[\\]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~{"[{]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~}"[}]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~<"[<]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~>"[>]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~"[~]* { return newToken(Terminals.TEXT, yytext().substring(1)); }
"~ " { return newToken(Terminals.HARDSPACE); }  // Going with HardSpace here
^[ \t]*"~"[*#=|] {
    int len = yylength();
    return newToken(Terminals.TEXT,
            yytext().substring(0, len - 2) + yytext().substring(len - 1));
}
^[ \t]*"~"---- {
    return newToken(Terminals.TEXT,
            yytext().substring(0, yylength() - 5) + "----");
}
// Only remaining special case is for escaping line breaks in TRs with | or ~.
// END of Escapes


// General/Global stuff
<YYINITIAL> [ \t]*\n { return newToken(Terminals.ROOTLVL_NEWLINE); }
<<EOF>> { return newToken(Terminals.EOF); }
// Following statement is for developers to TEMPORARY enable for debugging.
//. { if (yylength() != 1) throw new CreoleParseException("Match length != 1 for '.'  UNMATCHED: [" + yytext() + ']', yychar, yyline, yycolumn); }


// PSTATE (Paragaph) stuff
// In YYINITIAL only, transition to PSTATE upon non-blank line
<ESCURL> . {
    yybegin(urlDeferringState);  // Already been pushed, if necessary
    return newToken(Terminals.TEXT, yytext());
}
// Following case prevent falsely identified URLs by not attempting to link if
// URL is internal to a word.
// Whenever a bare URL occurs in a position where it wouldn't be linked, the
// user must escape the "//" with ~, or the parser will abort.
<YYINITIAL> [0-9a-zA-Z] / (https|http|ftp):"/"{S}*{NONPUNC} {
    pushState();
    urlDeferringState = PSTATE;
    yybegin(ESCURL);
    return newToken(Terminals.TEXT, yytext());
}
[0-9a-zA-Z] / (https|http|ftp):"/"{S}*{NONPUNC} {  // YYINITIAL handled already
    urlDeferringState = yystate();
    yybegin(ESCURL);
    return newToken(Terminals.TEXT, yytext());
}
// In PSTATE we write TEXT tokens until we encounter a blank line
<JCXBLOCKSTATE, PSTATE> [^] { return newToken(Terminals.TEXT, yytext()); }
"//" { return newToken(Terminals.EM_TOGGLE); }  // YYINITIAL handled already
## { return newToken(Terminals.MONO_TOGGLE); }  // YYINITIAL handled already
---- { yypushback(2); return newToken(Terminals.STRIKE_TOGGLE); }  // YYINITIAL handled already
-- { return newToken(Terminals.STRIKE_TOGGLE); }  // YYINITIAL handled already
__ { return newToken(Terminals.UNDER_TOGGLE); }  // YYINITIAL handled already
"^^" { return newToken(Terminals.SUP_TOGGLE); }  // YYINITIAL handled already
,, { return newToken(Terminals.SUB_TOGGLE); }  // YYINITIAL handled already
"**" { return newToken(Terminals.STRONG_TOGGLE); } // YYINITIAL handled already
// End PSTATE to make way for another element:
<PSTATE> \n / ("{{{" \n) {
    yybegin(popState());
    return newToken(Terminals.END_PARA);
}
<PSTATE> \n / [ \t]*= {
    yybegin(popState());
    return newToken(Terminals.END_PARA);
}
<PSTATE> \n / [ \t]*----[ \t]*\n {
    yybegin(popState());
    return newToken(Terminals.END_PARA);
}
<PSTATE> <<EOF>> { yybegin(popState()); return newToken(Terminals.END_PARA); }


// Misc Creole Inline elements.  May occur inside of Paras, TRs, LIs
\\\\ { return newToken(Terminals.HARDLINE); }
// I am violating Creole rules here and will not honor any markup inside the
// URL that we are linking.  I see no benefit to ever doing that.
<YYINITIAL> "~" (https|http|ftp):"/"{S}*{NONPUNC} {
    pushState();
    yybegin(PSTATE);
    return newToken(Terminals.TEXT, yytext().substring(1));
}
"~" (https|http|ftp):"/"{S}*{NONPUNC} {
    return newToken(Terminals.TEXT, yytext().substring(1));
}
<JCXBLOCKSTATE, PSTATE, LISTATE, TABLESTATE, HEADSTATE, DLSTATE>
(https|http|ftp):"/"{S}*{NONPUNC} {
    return newToken(Terminals.URL, yytext());
}
<YYINITIAL> ^ (https|http|ftp):"/"{S}*{NONPUNC} {
    pushState();
    yypushback(yylength());
    yybegin(PSTATE);
}
// Creole spec does not allow for https!!
"[[" ~ "]]" {
    // The optional 2nd half may in fact be a {{image}} instead of the target
    // URL.  In that case, the parser will handle it.
    // We delimit label from url with 0 char.
    StringBuilder sb = new StringBuilder(yytext());
    if (yystate() == YYINITIAL) {
        pushState();
        yybegin(PSTATE);
    }
    return newToken(Terminals.URL,
            sb.substring(2, yylength()-2), sb.indexOf("|") - 2);
}
"{{" ~ "}}" {
    if (yystate() == YYINITIAL) {
        pushState();
        yybegin(PSTATE);
    }
    // N.b. we handle images inside of [[links]] in the awkwardly redundant
    // way of parsing that out inside the parser instead of the scanner.
    // We delimit url from alttext with 0 char.
    StringBuilder sb = new StringBuilder(yytext());
    return newToken(Terminals.IMAGE,
            sb.substring(2, yylength()-2), sb.indexOf("|") - 2);
}


<JCXBLOCKSTATE, YYINITIAL> ^[ \t]*----[ \t]*\n {
    return newToken(Terminals.HOR);
}

--- { return newToken(Terminals.EMDASH); }

"<<<" | ">>>" {
    throw new CreoleParseException("'<<<' or '>>>' are reserved tokens",
            yychar, yyline, yycolumn);
}

// PLUGINs.  Must leave these below the <<< matcher I think.
<JCXBLOCKSTATE, YYINITIAL> ^[ \t]*"<<"[ \t]*styleSheet ~ ">>"[ \t]*\n {
    return newToken(Terminals.ROOTLVL_STYLESHEET,
            matcher(ParamPluginPattern, true).group(2));
}
^[ \t]*"<<"[ \t]*styleSheet ~ ">>"[ \t]*\n {
    return newToken(Terminals.NESTED_STYLESHEET,
            matcher(ParamPluginPattern, true).group(2));
}
<JCXBLOCKSTATE, YYINITIAL> ^[ \t]*"<<"[ \t]*enumFormats ~ ">>"[ \t]*\n {
    return newToken(Terminals.ROOTLVL_ENUMFORMATS,
            matcher(ParamPluginPattern, true).group(2));
}
^[ \t]*"<<"[ \t]*enumFormats ~ ">>"[ \t]*\n {
    return newToken(Terminals.NESTED_ENUMFORMATS,
            matcher(ParamPluginPattern).group(2));
}
<HEADSTATE> "<<"[ \t]*enumFormatReset ~ ">>" {
    return newToken(Terminals.ENUMFORMATRESET,
            matcher(ParamPluginPattern).group(2));
}
<PSTATE> "<<"[ \t]*footNoteEntry ~ ">>" {
    return newToken(Terminals.ENTRYDEF,
            matcher(ParamPluginPattern).group(2), 0);
}
<PSTATE> "<<"[ \t]*masterDefEntry ~ ">>" {
    return newToken(Terminals.ENTRYDEF,
            matcher(ParamPluginPattern).group(2), 1);
}
<JCXBLOCKSTATE, PSTATE, LISTATE, TABLESTATE, HEADSTATE, DLSTATE>
"<<"{s}*[}]{s}*">>" {
    return newToken(Terminals.END_JCXSPAN);
}
<JCXBLOCKSTATE, PSTATE, LISTATE, TABLESTATE, HEADSTATE, DLSTATE>
"<<"{s}*[{]{wsdash}*">>" {
    Matcher m = matcher(JcxPattern);
    if (m.groupCount() != 2)
        throw new RuntimeException(
                "JCX Matcher captured " + m.groupCount() + " groups");
    String classNames = m.group(2);
    return newToken(Terminals.JCXSPAN,
            (classNames == null) ? null : classNames.replaceAll("\\s+", " "));
}
<JCXBLOCKSTATE, PSTATE, LISTATE, TABLESTATE, HEADSTATE, DLSTATE>
"<<"{s}*"("{s}*">>" ~ "<<"{s}*")"{s}*">>" {
    Matcher m = matcher(IndexSandwichPattern);
    if (m.groupCount() != 2)
        throw new RuntimeException(
            "Index Sandwich Pattern captured " + m.groupCount() + " groups");
    if (needIndexCloser)
        throw new CreoleParseException(
                "Tangled index entry marker (...) pairing",
                yychar, yyline, yycolumn);
    needIndexCloser = true;
    yypushback(yylength() - m.group(1).length());
    return newToken(Terminals.INDEXED, m.group(2));
}
"<<"{s}*")"{s}*">>" {
    if (!needIndexCloser)
        throw new CreoleParseException(
                "Tangled index entry marker (...) pairing",
                yychar, yyline, yycolumn);
    needIndexCloser = false;
}
"<<"{s}*# ~ ">>" {}  // PLUGIN: Author comment
<PSTATE, HEADSTATE>
"<<"{s}*addClass[ \t]+[-=+]("block"|"inline"|"jcxSpan"){s}+{wsdash}+">>" {
    return newToken(Terminals.STYLER, matcher(ParamPluginPattern).group(2));
}
<JCXBLOCKSTATE, LISTATE, TABLESTATE, DLSTATE>
"<<"{s}*addClass[ \t]+[-=+]
("block"|"inline"|"jcxSpan"|"jcxBlock"){s}+{wsdash}+">>" {
    return newToken(Terminals.STYLER, matcher(ParamPluginPattern).group(2));
}
<PSTATE, HEADSTATE, JCXBLOCKSTATE, LISTATE, TABLESTATE, DLSTATE>
"<<"{s}*footNote[ \t]+[^>]+">>" {
    return newToken(Terminals.FOOTREF, matcher(ParamPluginPattern).group(2));
}
<PSTATE, HEADSTATE, JCXBLOCKSTATE, LISTATE, TABLESTATE, DLSTATE>
"<<"{s}*indexed[ \t]+[^>]+">>" {
    return newToken(Terminals.INDEXED, matcher(ParamPluginPattern).group(2));
}


// LISTATE stuff
<LISTATE> \n / [ \t]*= {
    yybegin(popState());
    return newToken(Terminals.FINAL_LI);
}
<LISTATE> . { return newToken(Terminals.TEXT, yytext()); }
<LISTATE> \n / [^] { return newToken(Terminals.TEXT, yytext()); }
<LISTATE> \n { }  // Ignore if last char in file
// End LISTATE to make way for another element:
<LISTATE> \n / ("{{{" \n) {
    yybegin(popState());
    return newToken(Terminals.FINAL_LI);
}
<LISTATE> \n / [ \t]*----[ \t]*\n {
    yybegin(popState());
    return newToken(Terminals.FINAL_LI);
}
<LISTATE> <<EOF>> { yybegin(popState()); return newToken(Terminals.FINAL_LI); }
// Would this last not defeat sending proper EOF to the parser?


// DLSTATE stuff
<DLSTATE> \n / [ \t]*= {
    yybegin(popState());
    return newToken(Terminals.FINAL_DT);
}
<DLSTATE> . { return newToken(Terminals.TEXT, yytext()); }
<DLSTATE> \n / [^] { return newToken(Terminals.TEXT, yytext()); }
<DLSTATE> \n { }  // Ignore if last char in file
// End DLSTATE to make way for another element:
<DLSTATE> \n / ("{{{" \n) {
    yybegin(popState());
    return newToken(Terminals.FINAL_DT);
}
<DLSTATE> \n / [ \t]*----[ \t]*\n {
    yybegin(popState());
    return newToken(Terminals.FINAL_DT);
}
<DLSTATE> <<EOF>> { yybegin(popState()); return newToken(Terminals.FINAL_DT); }
// Would this last not defeat sending proper EOF to the parser?


// TABLE stuff
<TABLESTATE> "~|" { return newToken(Terminals.TEXT, "|"); }
<TABLESTATE> "|~" / = { return newToken(Terminals.CELL, null); }
<TABLESTATE> "|=" { return newToken(Terminals.CELL, null, 1); }
  // 1 is the SOH character code for "Start Of Header"
<TABLESTATE> "|" { return newToken(Terminals.CELL); }
<TABLESTATE> . { return newToken(Terminals.TEXT, yytext()); }
<TABLESTATE> <<EOF>> {
    yybegin(popState());
    return newToken(Terminals.FINAL_ROW);
}
// I believe that the following can only be called if very last thing in file,
// since above we have captured both "\n\s*[|]" and "\n\s*[^|]".
<TABLESTATE> \n { yybegin(popState()); return newToken(Terminals.FINAL_ROW); }


// HEADING STUFF
<HEADSTATE> ^[ \t]*=+ {
    int hLevel = yytext().trim().length();
    if (hLevel < 1 || hLevel > 6)
        throw new CreoleParseException(
                "Unexpected level for Heading prefix: " + yytext().trim(),
                yychar, yyline, yycolumn);
    return newToken(Terminals.HEADING, null, hLevel);
}
<HEADSTATE> (=+[ \t]*)? \n {
    // No need to pop state.  We always come from and return to YYINITIAL.
    yybegin(YYINITIAL);
    return newToken(Terminals.END_H);
}
<HEADSTATE> . { return newToken(Terminals.TEXT, yytext()); }

"<<" {
    throw new CreoleParseException("Unknown plugin", yychar, yyline, yycolumn);
}
