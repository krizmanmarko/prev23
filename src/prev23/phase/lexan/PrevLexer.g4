lexer grammar PrevLexer;

@header {
	package prev23.phase.lexan;
	import prev23.common.report.*;
	import prev23.data.sym.*;
}

@members {
    @Override
	public Token nextToken() {
		return (Token) super.nextToken();
	}
}


///////////////////////////////////////////////////////////////////////////
// constants
///////////////////////////////////////////////////////////////////////////

VOID_CONST : 'none' ;
BOOL_CONST: 'true' | 'false' ;

INT_CONST : '0'
	| [1-9]([0-9])*
	| '0'[0-9]+ {
		if (true) { // avoid unreachable statement problem
			String msg = "'0' cannot be leading character";
			int begLine = _tokenStartLine;
			int begColumn = _tokenStartCharPositionInLine;
			int endLine = getLine();
			int endColumn = getCharPositionInLine();
			Location loc = new Location(begLine, begColumn,
						    endLine, endColumn);
			throw new Report.Error(loc, msg);
		}
	} ;

// special handling for \' and empty constant and 'aa...'
CHAR_CONST : '\''('\\\''|[ -&(-~])'\''
	| '\'\'' {
		if (true) {
			String msg = "Illegal character constant ''";
			int begLine = _tokenStartLine;
			int begColumn = _tokenStartCharPositionInLine;
			int endLine = getLine();
			int endColumn = getCharPositionInLine();
			Location loc = new Location(begLine, begColumn,
						    endLine, endColumn);
			throw new Report.Error(loc, msg);
		}
	}
	| '\''('\\\''|[ -&(-~])('\\\''|[ -&(-~])+'\'' {
		if (true) {
			String msg = "Character constant too long";
			int begLine = _tokenStartLine;
			int begColumn = _tokenStartCharPositionInLine;
			int endLine = getLine();
			int endColumn = getCharPositionInLine();
			Location loc = new Location(begLine, begColumn,
						    endLine, endColumn);
			throw new Report.Error(loc, msg);
		}
	} ;

// special handling for \"
STRING_CONST : '"'('\\"'|[ -!#-~])*'"'
	| '"'('\\"'|[ -!#-~])* {
		if (true) {
			String msg = "EOF while scanning string literal (\" missing?)";
			int begLine = _tokenStartLine;
			int begColumn = _tokenStartCharPositionInLine;
			int endLine = getLine();
			int endColumn = getCharPositionInLine();
			Location loc = new Location(begLine, begColumn,
						    endLine, endColumn);
			throw new Report.Error(loc, msg);
		}
	} ;

PTR_CONST : 'nil' ;
 

///////////////////////////////////////////////////////////////////////////
// keywords
///////////////////////////////////////////////////////////////////////////

BOOL : 'bool' ;
CHAR : 'char' ;
DEL : 'del' ;
DO : 'do' ;
ELSE : 'else' ;
FUN : 'fun' ;
IF : 'if' ;
IN : 'in' ;
INT : 'int' ;
LET : 'let' ;
NEW : 'new' ;
THEN : 'then' ;
TYP : 'typ' ;
VAR : 'var' ;
VOID : 'void' ;
WHILE : 'while' ;


///////////////////////////////////////////////////////////////////////////
// symbols
///////////////////////////////////////////////////////////////////////////

LPAR : '(' ;
RPAR : ')' ;
LBRACE : '{' ;
RBRACE : '}' ;
LBRACKET : '[' ;
RBRACKET : ']' ;
DOT : '.' ;
COMMA : ',' ;
COLON : ':' ;
SEMICOLON : ';' ;
AMPERSAND : '&' ;
PIPE : '|' ;
EXCLAM : '!' ;
EQ : '==' ;
NE : '!=' ;
LT : '<' ;
GT : '>' ;
LE : '<=' ;
GE : '>=' ;
ASTERISK : '*' ;
SLASH : '/' ;
PERCENT : '%' ;
PLUS : '+' ;
MINUS : '-' ;
CARON : '^' ;
EQUAL : '=' ;


///////////////////////////////////////////////////////////////////////////
// identifiers
///////////////////////////////////////////////////////////////////////////

ID : [a-zA-Z_][a-zA-Z0-9_]* ;


///////////////////////////////////////////////////////////////////////////
// comments
///////////////////////////////////////////////////////////////////////////

COMMENT : '#'(~[\n])*'\n' -> skip;


///////////////////////////////////////////////////////////////////////////
// whitespace
///////////////////////////////////////////////////////////////////////////

// fix tab and lexer communication
WHITESPACE : ([ \n\r]
	| '\t' {
		int new_pos = _tokenStartCharPositionInLine;
		new_pos += 8 - new_pos % 8;
		setCharPositionInLine(new_pos);
	}) -> skip ;
