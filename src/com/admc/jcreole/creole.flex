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
    private static final Pattern HeadingPattern =
            Pattern.compile("\\s*(=+)(.*?)(?:=+\\s*)?\n");
    private static final Pattern ListLevelPattern =
            Pattern.compile("\\s*([#*]+)");

    private Token newToken(short id) {
        return new Token(id, null, yychar, yyline, yycolumn);
    }
    private Token newToken(short id, String s) {
        return new Token(id, s, yychar, yyline, yycolumn);
    }
    private Token newToken(short id, String s, int intParam) {
        return new Token(id, s, yychar, yyline, yycolumn, intParam);
    }

    private int pausingState, listLevel;

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
        return new CreoleScanner(new CharSequenceReader(sb));
    }
%}

%states PSTATE, LISTATE, ESCURL

S = [^ \t\f\n]
NONPUNC = [^ \t\f\n,.?!:;\"']  // Allowed last character of URLs.
%%

// Force high-priorioty of these very short captures
// len 1:
// Transition to LISTATE from any state other than LISTATE
<YYINITIAL> ^[ \t]*[#*] { yypushback(1); yybegin(LISTATE); }
<PSTATE> ^[ \t]*[#*] { yypushback(1); yybegin(LISTATE); return newToken(Terminals.END_PARA); }
// len >= 1:
<PSTATE> ^([ \t]*\n)+ { yybegin(YYINITIAL); yypushback(1); return newToken(Terminals.END_PARA); }
<LISTATE> \n / [ \t]*\n { yybegin(YYINITIAL); return newToken(Terminals.FINAL_LI); }
<LISTATE> \n / [ \t]*[#*] { return newToken(Terminals.END_LI, null, listLevel); }
<LISTATE> ^[ \t]*((#+)|("*"+)) {
    Matcher m = ListLevelPattern.matcher(yytext());
    m.matches();
    return newToken(Terminals.LI,
            Character.toString(m.group(1).charAt(0)), m.group(1).length());
}

^("{{{"\n) ~ (\n"}}}"\n) {
    Matcher m = BlockPrePattern.matcher(yytext());
    if (!m.matches())
        throw new CreoleParseException(
            "BLOCK_PRE text doesn't match our block noWiki pattern: \""
            + yytext() + '"', yychar, yyline, yycolumn);
    return newToken(Terminals.BLOCK_PRE, m.group(1));
}
"{{{" ~ ("}"* "}}}") {
    Matcher m = InlinePrePattern.matcher(yytext());
    if (!m.matches())
        throw new CreoleParseException(
            "INLINE_PRE text doesn't match our inline noWiki pattern: \""
            + yytext() + '"', yychar, yyline, yycolumn);
    return newToken(Terminals.INLINE_PRE, m.group(1));
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
"~**" { return newToken(Terminals.TEXT, "**"); }
"~//" { return newToken(Terminals.TEXT, "//"); }
"~[[" { return newToken(Terminals.TEXT, "[["); }
"~]]" { return newToken(Terminals.TEXT, "]]"); }
"~\\\\" { return newToken(Terminals.TEXT, "\\\\"); }
"~{{" { return newToken(Terminals.TEXT, "{{"); }
"~}}" { return newToken(Terminals.TEXT, "}}"); }
"~~" { return newToken(Terminals.TEXT, "~"); }
"~ " { return newToken(Terminals.HARDSPACE); }  // Going with HardSpace here
// TODO:  I believe that these will cause the next token to inherit the ^:
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
<YYINITIAL> \n {}  // Ignore newlines at root state.
<<EOF>> { return newToken(Terminals.EOF); }
//. { if (yylength() != 1) throw new CreoleParseException("Match length != 1 for '.'  UNMATCHED: [" + yytext() + ']', yychar, yyline, yycolumn); }


// PSTATE (Paragaph) stuff
// In YYINITIAL only, transition to PSTATE upon non-blank line
<ESCURL> . { yybegin(pausingState); return newToken(Terminals.TEXT, yytext()); }
<YYINITIAL> . { yybegin(PSTATE); return newToken(Terminals.TEXT, yytext()); }
<YYINITIAL> "//" { yybegin(PSTATE); return newToken(Terminals.EM_TOGGLE); }
<YYINITIAL> "**" { yybegin(PSTATE); return newToken(Terminals.STRONG_TOGGLE); }
// Following case prevent falsely identified URLs by no attempting to link if
// URL is internal to a word.
// Whenever a bare URL occurs in a position where it wouldn't be linked, the
// user must escape the "//" with ~, or the parser will abort.
<YYINITIAL, PSTATE> [0-9a-zA-Z] / (https|http|ftp):"/"{S}*{NONPUNC} {
    pausingState = PSTATE;
    yybegin(ESCURL);
    return newToken(Terminals.TEXT, yytext());
}
<LISTATE> [0-9a-zA-Z] / (https|http|ftp):"/"{S}*{NONPUNC} {
    pausingState = LISTATE;
    yybegin(ESCURL);
    return newToken(Terminals.TEXT, yytext());
}
// In PSTATE we write TEXT tokens until we encounter a blank line
<PSTATE> [^] { return newToken(Terminals.TEXT, yytext()); }
<PSTATE, LISTATE> "//" { return newToken(Terminals.EM_TOGGLE); }
<PSTATE, LISTATE> "**" { return newToken(Terminals.STRONG_TOGGLE); }
// End PSTATE to make way for another element:
<PSTATE> \n / ("{{{" \n) { yybegin(YYINITIAL); return newToken(Terminals.END_PARA); }
<PSTATE> \n / [ \t]*= { yybegin(YYINITIAL); return newToken(Terminals.END_PARA); }
<PSTATE> \n / [ \t]*----[ \t]*\n { yybegin(YYINITIAL); return newToken(Terminals.END_PARA);}
<PSTATE> <<EOF>> { yybegin(YYINITIAL); return newToken(Terminals.END_PARA); }


// Misc Creole Inline elements.  May occur inside of Paras, TRs, LIs
\\\\ { return newToken(Terminals.HARDLINE); }
// I am violating Creole rules here and will not honor any markup inside the
// URL that we are linking.  I see no benefit to ever doing that.
<YYINITIAL> "~" (https|http|ftp):"/"{S}*{NONPUNC} { yybegin(PSTATE); return newToken(Terminals.TEXT, yytext().substring(1)); }
"~" (https|http|ftp):"/"{S}*{NONPUNC} { return newToken(Terminals.TEXT, yytext().substring(1)); }
<PSTATE, LISTATE> (https|http|ftp):"/"{S}*{NONPUNC} { return newToken(Terminals.URL, yytext()); }
// Creole spec does not allow for https!!
"[[" ~ "]]" {
    // The optional 2nd half may in fact be a {{image}} instead of the target
    // URL.  In that case, the parser will handle it.
    // We delimit label from url with 0 char.
    StringBuilder sb = new StringBuilder(yytext());
    return newToken(Terminals.URL,
            sb.substring(2, yylength()-2), sb.indexOf("|") - 2);
}
"{{" ~ "}}" {
    // N.b. we handle images inside of [[links]] in the awkwardly redundant
    // way of parsing that out inside the parser instead of the scanner.
    // We delimit url from alttext with 0 char.
    StringBuilder sb = new StringBuilder(yytext());
    return newToken(Terminals.IMAGE,
            sb.substring(2, yylength()-2), sb.indexOf("|") - 2);
}

// Misc Creole Block elements
// (which require special characters at beginning of line)
// These have a paired PSTATE rule above which will close a (possible PSTATE),
// preparing for the YYINITIAL state rules here.
<YYINITIAL> ^[ \t]*= ~ \n {
    Matcher m = HeadingPattern.matcher(yytext());
    if (!m.matches())
        throw new CreoleParseException(
            "Header line text doesn't match our Heading pattern: \""
            + yytext() + '"', yychar, yyline, yycolumn);
    String headerText = m.group(2);
    int hLevel = m.group(1).length();
    if (hLevel < 1 || hLevel > 6)
        throw new CreoleParseException(
                "Unexpected level for Heading command: " + yytext(),
                yychar, yyline, yycolumn);
    return newToken(Terminals.HEADING, headerText, hLevel);
}
<YYINITIAL> ^[ \t]*----[ \t]*\n { return newToken(Terminals.HOR); }

"<<<" | ">>>" {
    throw new CreoleParseException("'<<<' or '>>>' are reserved tokens",
            yychar, yyline, yycolumn);
}


// LISTATE stuff
<LISTATE> . { return newToken(Terminals.TEXT, yytext()); }
<LISTATE> \n / [^] { return newToken(Terminals.TEXT, yytext()); }
<LISTATE> \n { }  // Ignore if last char in file
// End LISTATE to make way for another element:
<LISTATE> \n / ("{{{" \n) { yybegin(YYINITIAL); return newToken(Terminals.FINAL_LI); }
<LISTATE> \n / [ \t]*= { yybegin(YYINITIAL); return newToken(Terminals.FINAL_LI); }
<LISTATE> \n / [ \t]*----[ \t]*\n { yybegin(YYINITIAL); return newToken(Terminals.FINAL_LI);}
<LISTATE> <<EOF>> { yybegin(YYINITIAL); return newToken(Terminals.FINAL_LI); }

//<TR> "~|" { return newToken(Terminals.TEXT, "|"); }
