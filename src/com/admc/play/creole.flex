package com.admc.play;

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
    private static Token tok(short id) { return new Token(id); }
    private static Token tok(short id, Character cr) {
        return new Token(id, cr);
    }
%}

UTF_EOL = \r|\n|\r\n|\u2028|\u2029|\u000B|\u000C|\u0085
%%

"~" { return tok(Terminals.ESCAPE); }
{UTF_EOL}  "}}}" { return tok(Terminals.NOWIKI_BLOCK_CLOSE); }
{UTF_EOL} { return tok(Terminals.NEWLINE); }

[ \t]+ { return tok(Terminals.BLANKS); }

":/" { return tok(Terminals.COLON_SLASH); }
"//" { return tok(Terminals.ITAL); }
"{{{" { return tok(Terminals.NOWIKI_OPEN); }
"}}}" { return tok(Terminals.NOWIKI_CLOSE); }
"[[" { return tok(Terminals.LINK_OPEN); }
"]]" { return tok(Terminals.LINK_CLOSE); }
"{{" { return tok(Terminals.IMAGE_OPEN); }
"}}" { return tok(Terminals.IMAGE_CLOSE); }
\\\\\\\\ { return tok(Terminals.FORCED_LINEBREAK); }
= { return tok(Terminals.EQUAL); }
'|' { return tok(Terminals.PIPE); }
# { return tok(Terminals.POUND); }
- { return tok(Terminals.DASH); }
"*" { return tok(Terminals.STAR); }
"/" { return tok(Terminals.SLASH); }
@@ { return tok(Terminals.EXTENSION); }
. { return tok(Terminals.NORMAL_CHAR, Character.valueOf(yytext().charAt(0))); }
