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
            Pattern.compile("(?s)\\Q{{{\\E\r?\n(.*?)\r?\n\\Q}}}\\E\r?\n");
    private static final Pattern InlinePrePattern =
            Pattern.compile("(?s)\\Q{{{\\E(.*?)\\Q}}}");
    private static final Pattern HeaderPattern =
            Pattern.compile("\\s*(=+)(.*?)(?:=+\\s*)?\r?\n");

    private Token newToken(short id) {
        return new Token(id, null, yychar, yyline, yycolumn);
    }
    private Token newToken(short id, String s) {
        return new Token(id, s, yychar, yyline, yycolumn);
    }
    private Token newToken(short id, String s, int intParam) {
        return new Token(id, s, yychar, yyline, yycolumn, intParam);
    }

    private int pausingState;

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
     *
     * Pass a StringBuilder containing any characters and we'll clean or
     * throw illegal characters depending on the doClean specification.
     *
     * @param doClean If true will silently remove illegal input characters.
     *                If false, will throw if encounter any illegal input char.
     */
    public static CreoleScanner newCreoleScanner(
            StringBuilder sb, boolean doClean) throws IOException {
        List<Integer> badIndexes = new ArrayList<Integer>();
        char c;
        for (int i = sb.length() - 1; i >= 0; i--) {
            c = sb.charAt(i);
            if (c != '\n' && c != '\r' && Character.isISOControl(c)) {
                if (doClean) sb.deleteCharAt(i);
                else badIndexes.add(badIndexes.size(), Integer.valueOf(i));
            }
        }
        if (badIndexes.size() > 0)
            throw new IllegalArgumentException(
                    "Illegal char(s) at following positions: " + badIndexes);
        return new CreoleScanner(new CharSequenceReader(sb));
    }
%}

%states PSTATE, LISTATE, ESCURL

UTF_EOL = (\r|\n|\r\n|\u2028|\u2029|\u000B|\u000C|\u0085)
DOT = [^\n\r]  // For some damned reason JFlex's "." does not exclude \r.
ALLBUTR = [^\r]  // For some damned reason JFlex's "." does not exclude \r.
S = [^ \t\f\n\r]
NONPUNC = [^ \t\f\n\r,.?!:;\"']  // Allowed last character of URLs.
%%

// Force high-priorioty of these very short captures
// len 1:
\r {}  // Eat \rs in all inclusive states
// Transition to LISTATE from any state other than LISTATE
<YYINITIAL, PSTATE> ^[ \t]*[#*] { yypushback(1); yybegin(LISTATE); }
// len >= 1:
<PSTATE> ^([ \t]*{UTF_EOL})+ { yybegin(YYINITIAL); yypushback(1); return newToken(Terminals.END_PARA); }  // TODO:  Pushback 1 or 2 depending on EOL chars.

^("{{{"{UTF_EOL}) ~ ({UTF_EOL}"}}}"{UTF_EOL}) {
    Matcher m = BlockPrePattern.matcher(yytext());
    if (!m.matches())
        throw new CreoleParseException(
            "BLOCK_PRE text doesn't match our pattern: \"" + yytext() + '"',
            yychar, yyline, yycolumn);
    return newToken(Terminals.BLOCK_PRE, m.group(1).replaceAll("\r", ""));
}
"{{{" ~ ("}"* "}}}") {
    Matcher m = InlinePrePattern.matcher(yytext());
    if (!m.matches())
        throw new CreoleParseException(
            "INLINE_PRE text doesn't match our pattern: \"" + yytext() + '"',
            yychar, yyline, yycolumn);
    return newToken(Terminals.INLINE_PRE, m.group(1).replaceAll("\r", ""));
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
^[ \t]*"~[*#=|]" {
    int len = yylength();
    return newToken(Terminals.TEXT,
    yytext().substring(len - 2) + yytext().substring(len - 1));
}
^[ \t]*"~"---- {
    return newToken(Terminals.TEXT, yytext().substring(yylength() - 5) + "----");
}
// Only remaining special case is for escaping line breaks in TRs with | or ~.
// END of Escapes


// General/Global stuff
<YYINITIAL> \n {}  // Ignore newlines at root state.
<<EOF>> { return newToken(Terminals.EOF); }
//. { if (yylength() != 1) throw new CreoleParseException("Match length != 1 for '.'  UNMATCHED: [" + yytext() + ']', yychar, yyline, yycolumn); }


// PSTATE (Paragaph) stuff
// In YYINITIAL only, transition to PSTATE upon non-blank line
<ESCURL> {DOT} { yybegin(pausingState); return newToken(Terminals.TEXT, yytext()); }
<YYINITIAL> {DOT} { yybegin(PSTATE); return newToken(Terminals.TEXT, yytext()); }
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
// In PSTATE we write TEXT tokens (incl. \r) until we encounter a blank line
<PSTATE> {ALLBUTR} { return newToken(Terminals.TEXT, yytext()); }
<PSTATE> "//" { return newToken(Terminals.EM_TOGGLE); }
<PSTATE> "**" { return newToken(Terminals.STRONG_TOGGLE); }
// End PSTATE to make way for another element:
<PSTATE> {UTF_EOL} / ("{{{" {UTF_EOL}) { yybegin(YYINITIAL); return newToken(Terminals.END_PARA); }
<PSTATE> {UTF_EOL} / [ \t]*= { yybegin(YYINITIAL); return newToken(Terminals.END_PARA); }
<PSTATE> {UTF_EOL} / [ \t]*----[ \t]*{UTF_EOL} { yybegin(YYINITIAL); return newToken(Terminals.END_PARA);}
<PSTATE> <<EOF>> { yybegin(YYINITIAL); return newToken(Terminals.END_PARA); }


// Misc Creole Inline elements.  May occur inside of Paras, TRs, LIs
\\\\ { return newToken(Terminals.HARDLINE); }
// I am violating Creole rules here and will not honor any markup inside the
// URL that we are linking.  I see no benefit to ever doing that.
"~" (https|http|ftp):"/"{S}*{NONPUNC} { yybegin(PSTATE); return newToken(Terminals.TEXT, yytext().substring(1)); }
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
<YYINITIAL> ^[ \t]*= ~ {UTF_EOL} {
    yybegin(YYINITIAL);
    Matcher m = HeaderPattern.matcher(yytext());
    if (!m.matches())
        throw new CreoleParseException(
            "Header line text doesn't match our pattern: \"" + yytext() + '"',
            yychar, yyline, yycolumn);
    String headerText = m.group(2);
    int hLevel = m.group(1).length();
    if (hLevel < 1 || hLevel > 6)
        throw new CreoleParseException(
                "Unexpected level for Heading command: " + yytext(),
                yychar, yyline, yycolumn);
    return newToken(Terminals.HEADING, headerText, hLevel);
}
<YYINITIAL> ^[ \t]*----[ \t]*{UTF_EOL} { yybegin(YYINITIAL); return newToken(Terminals.HOR); }

"<<<" | ">>>" {
    throw new CreoleParseException("'<<<' or '>>>' are reserved tokens",
            yychar, yyline, yycolumn);
}


// LISTATE (Paragaph) stuff
<LISTATE> ^[ \t]*#+ { return newToken(Terminals.OLI); }
<LISTATE> ^[ \t]*"*"+ { return newToken(Terminals.ULI); }
<LISTATE> {ALLBUTR} { return newToken(Terminals.TEXT, yytext()); }
// End LISTATE to make way for another element:
<LISTATE> {UTF_EOL} / [ \t]*[#*] { return newToken(Terminals.END_LI); }
<LISTATE> {UTF_EOL} / [ \t]*{UTF_EOL} { yybegin(YYINITIAL); return newToken(Terminals.FINAL_LI); }
<LISTATE> {UTF_EOL} / ("{{{" {UTF_EOL}) { yybegin(YYINITIAL); return newToken(Terminals.FINAL_LI); }
<LISTATE> {UTF_EOL} / [ \t]*= { yybegin(YYINITIAL); return newToken(Terminals.FINAL_LI); }
<LISTATE> {UTF_EOL} / [ \t]*----[ \t]*{UTF_EOL} { yybegin(YYINITIAL); return newToken(Terminals.FINAL_LI);}
<LISTATE> <<EOF>> { yybegin(YYINITIAL); return newToken(Terminals.FINAL_LI); }

//<TR> "~|" { return newToken(Terminals.TEXT, "|"); }
