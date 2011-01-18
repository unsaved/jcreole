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
%eofval{
    return new Token(Terminals.EOF);
%eofval}

%{
    private static final Pattern BlockPrePattern =
            Pattern.compile("\\Q{{{\\E\r?\n(.*?)\r?\n\\Q}}}");
    private static final Pattern InlinePrePattern =
            Pattern.compile("\\Q{{{\\E(.*?)\\Q}}}");
    private static Token tok(short id) { return new Token(id); }
    private static Token tok(short id, String s) {
        return new Token(id, s);
    }
%}

//%states TR

UTF_EOL = \r|\n|\r\n|\u2028|\u2029|\u000B|\u000C|\u0085
DOT = [^\n\r]  // For some damned reason JFlex's "." does not exclude \r.
%%

^"{{{$" ~ {UTF_EOL}"}}}"$ {
    Matcher m = BlockPrePattern.matcher(yytext());
    if (!m.matches())
        throw new IllegalStateException(
                "BLOCK_PRE text doesn't match our pattern: " + yytext());
    return tok(Terminals.BLOCK_PRE, m.group(1));
}
"{{{$" ~ "}}}" {
    Matcher m = InlinePrePattern.matcher(yytext());
    if (!m.matches())
        throw new IllegalStateException(
                "INLINE_PRE text doesn't match our pattern: " + yytext());
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
^[ \t]*~---- {
    int len = yytext().length();
    return tok(Terminals.TEXT, yytext().substring(len - 5) + "----");
}
^[ \t]*~"{{{" {
    int len = yytext().length();
    return tok(Terminals.TEXT, yytext().substring(len - 4) + "{{{");
}
^[ \t]*~"}}}" {
    int len = yytext().length();
    return tok(Terminals.TEXT, yytext().substring(len - 4) + "}}}");
}
//<TR> "~|" { return tok(Terminals.TEXT, "|"); }
^([ \t]*{UTF_EOL})+ { yybegin(YYINITIAL); return tok(Terminals.PARAGRAPH); }
\r {}
<YYINITIAL> {DOT} { return tok(Terminals.TEXT, yytext()); }
