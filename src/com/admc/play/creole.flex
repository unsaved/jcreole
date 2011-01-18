package com.admc.play;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

%%
%class CreoleScanner
%public
%unicode
%eofclose
%line
%column
%extends beaver.Scanner
%function nextToken
%type Token

%{
    private static final Pattern BlockPrePattern =
            Pattern.compile("(?s)\\Q{{{\\E\r?\n(.*?)\r?\n\\Q}}}\\E\r?\n");
    private static final Pattern InlinePrePattern =
            Pattern.compile("(?s)\\Q{{{\\E(.*?)\\Q}}}");
    private static Token tok(short id) { return new Token(id); }
    private static Token tok(short id, String s) {
        return new Token(id, s);
    }
%}

//%states TR
%states PSTATE
//%xstates HSTATE

UTF_EOL = (\r|\n|\r\n|\u2028|\u2029|\u000B|\u000C|\u0085)
DOT = [^\n\r]  // For some damned reason JFlex's "." does not exclude \r.
ALLBUTR = [^\r]  // For some damned reason JFlex's "." does not exclude \r.
%%

^("{{{"{UTF_EOL}) ~ ({UTF_EOL}"}}}"{UTF_EOL}) {
    Matcher m = BlockPrePattern.matcher(yytext());
    if (!m.matches())
        throw new IllegalStateException(
            "BLOCK_PRE text doesn't match our pattern: \"" + yytext() + '"');
    return tok(Terminals.BLOCK_PRE, m.group(1));
}
"{{{" ~ "}}}" {
    Matcher m = InlinePrePattern.matcher(yytext());
    if (!m.matches())
        throw new IllegalStateException(
            "INLINE_PRE text doesn't match our pattern: \"" + yytext() + '"');
    return tok(Terminals.INLINE_PRE, m.group(1));
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
// Ah, I see that theyu missed ^"*".
// ANYWHERES
"~**" { return tok(Terminals.TEXT, "**"); }
"~//" { return tok(Terminals.TEXT, "//"); }
"~[[" { return tok(Terminals.TEXT, "[["); }
"~]]" { return tok(Terminals.TEXT, "]]"); }
"~\\\\" { return tok(Terminals.TEXT, "\\\\"); }
"~{{" { return tok(Terminals.TEXT, "{{"); }
"~}}" { return tok(Terminals.TEXT, "}}"); }
"~~" { return tok(Terminals.TEXT, "~"); }
"~ " { return tok(Terminals.HARDSPACE); }  // Going with HardSpace here
// TODO:  I believe that these will cause the next token to inherit the ^:
^[ \t]*"~[*#=|]" {
    int len = yytext().length();
    return tok(Terminals.TEXT,
    yytext().substring(len - 2) + yytext().substring(len - 1));
}
^[ \t]*"~"---- {
    int len = yytext().length();
    return tok(Terminals.TEXT, yytext().substring(len - 5) + "----");
}
^[ \t]*"~{{{" {
    int len = yytext().length();
    return tok(Terminals.TEXT, yytext().substring(len - 4) + "{{{");
}
^[ \t]*"~}}}" {
    int len = yytext().length();
    return tok(Terminals.TEXT, yytext().substring(len - 4) + "}}}");
}
//<TR> "~|" { return tok(Terminals.TEXT, "|"); }

// In YYINITIAL only, transition to PSTATE upon non-blank line
<YYINITIAL> {DOT} { yybegin(PSTATE); return tok(Terminals.TEXT, yytext()); }

// In PSTATE we write TEXT tokens (incl. \r) until we encounter a blank line
<PSTATE> ^([ \t]*{UTF_EOL})+ { yybegin(YYINITIAL); return tok(Terminals.END_PARA); }
<PSTATE> {UTF_EOL} / ("{{{" {UTF_EOL}) { return tok(Terminals.END_PARA); }
<PSTATE> {ALLBUTR} { return tok(Terminals.TEXT, yytext()); }

\r {}  // Eat \rs in all inclusive states
<YYINITIAL> \n {}  // Ignore newlines at root state.
//\n { if (yytext().length() != 1) throw new IllegalStateException("Match length != 1 for '\\n'"); System.err.println("UNMATCHED Newline @ " + yyline + ':' + (yycolumn+1)); }
//. { if (yytext().length() != 1) throw new IllegalStateException("Match length != 1 for '.'"); System.err.println("UNMATCHED: [" + yytext() + "] @ "+ yyline + ':' + (yycolumn+1)); }
<PSTATE> <<EOF>> { yybegin(YYINITIAL); return tok(Terminals.END_PARA); }
<<EOF>> { return tok(Terminals.EOF); }
