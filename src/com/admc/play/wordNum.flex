package com.admc.play;

import java.io.IOException;

%%
%class Scanner
// %int
// %debug
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
s = [ \t\f\n\r]
S = [^ \t\f\n\r]
UTF_EOL = \r|\n|\r\n|\u2028|\u2029|\u000B|\u000C|\u0085
DOT = [^\r|\n|\r\n|\u2028|\u2029|\u000B|\u000C|\u0085]
%%
//{s}+ { return new Symbol(sym.WHITESPACE, yytext()); }
//{S}+ { return new Symbol(sym.WORD, yytext());}
{s}+ { }
[:digit:]+ {
    //Symbol t = new Symbol(Terminals.NUMBER, yytext());
    Token t = new Token(Terminals.NUMBER, Integer.parseInt(yytext()));
    return t;
}
{S}+ {
    //Symbol t = new Symbol(Terminals.WORD, yytext());
    Token t = new Token(Terminals.WORD, yytext());
    yypushback(-1);
    return t;
}
//{UTF_EOL} { }
/*
\n | . { System.err.println("UNMATCHED: [" + yytext() + ") len=" + yytext().length()); }
*/
