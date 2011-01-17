package com.admc.play;

//enum TokenType { WHITESPACE, WORD };
enum TokenType { BLANKLINE, LINE };

class Yytoken {
    public Yytoken(TokenType t, String s) { type = t; value = s; }
    public TokenType type;
    public String value;

    public String toString() {
        return type.toString() + "/(" + value + ')';
    }
}

%%
%class Scanner
// %int
// %debug
%public
%unicode
%eofclose
%line
%column
//s = [ \t\f\n\r]
//S = [^ \t\f\n\r]
UTF_EOL = \r|\n|\r\n|\u2028|\u2029|\u000B|\u000C|\u0085
DOT = [^\r|\n|\r\n|\u2028|\u2029|\u000B|\u000C|\u0085]
%%
//{s}+ { return new Yytoken(TokenType.WHITESPACE, yytext()); }
//{S}+ { return new Yytoken(TokenType.WORD, yytext());}
^ [ \t]+ $ {
    Yytoken t = new Yytoken(TokenType.BLANKLINE, yytext());
    yypushback(-1);
    return t;
}
^{DOT}+$ {
    Yytoken t = new Yytoken(TokenType.LINE, yytext());
    yypushback(-1);
    return t;
}
{UTF_EOL} { return new Yytoken(TokenType.BLANKLINE, yytext()); }
/*
\n | . { System.err.println("UNMATCHED: [" + yytext() + ") len=" + yytext().length()); }
*/
