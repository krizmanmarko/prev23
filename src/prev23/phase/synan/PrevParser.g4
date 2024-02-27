parser grammar PrevParser;

@header {

	package prev23.phase.synan;

	import java.util.*;

	import prev23.common.report.*;
	import prev23.phase.lexan.*;

	import prev23.data.ast.attribute.*;
	import prev23.data.ast.tree.decl.*;
	import prev23.data.ast.tree.expr.*;
	import prev23.data.ast.tree.type.*;
	import prev23.data.ast.tree.stmt.*;
	import prev23.data.ast.tree.*;
	import prev23.data.ast.visitor.*;
}

@members {

	private Location loc(Token tok) {
		return new Location((prev23.data.sym.Token) tok);
	}

	private Location loc(Locatable loc) {
		return new Location(loc);
	}

	private Location loc(Token tok1, Token tok2) {
		return new Location((prev23.data.sym.Token) tok1,
				    (prev23.data.sym.Token) tok2);
	}

	private Location loc(Token tok1, Locatable loc2) {
		return new Location((prev23.data.sym.Token) tok1, loc2);
	}

	private Location loc(Locatable loc1, Token tok2) {
		return new Location(loc1, (prev23.data.sym.Token) tok2);
	}

	private Location loc(Locatable loc1, Locatable loc2) {
		return new Location(loc1, loc2);
	}
}

options{
    tokenVocab=PrevLexer;
}

source
  returns [AstTree ast]
  : declarations EOF { $ast = $declarations.ast; }
  ;

declarations
  returns [AstTree ast]
  : type_declarations declarations_1
  {
	LinkedList<AstTree> ll = $declarations_1.ll;
	ll.addFirst($type_declarations.ast);
	$ast = new AstTrees<AstTree>("declarations", ll);
  }
  | function_declarations declarations_1
  {
	LinkedList<AstTree> ll = $declarations_1.ll;
	ll.addFirst($function_declarations.ast);
	$ast = new AstTrees<AstTree>("declarations", ll);
  }
  | variable_declarations declarations_1
  {
	LinkedList<AstTree> ll = $declarations_1.ll;
	ll.addFirst($variable_declarations.ast);
	$ast = new AstTrees<AstTree>("declarations", ll);
  }
  ;

declarations_1
  returns [LinkedList<AstTree> ll]
  : type_declarations declarations_1
  {
	$ll = $declarations_1.ll;
	$ll.addFirst($type_declarations.ast);
  }
  | function_declarations declarations_1
  {
	$ll = $declarations_1.ll;
	$ll.addFirst($function_declarations.ast);
  }
  | variable_declarations declarations_1
  {
	$ll = $declarations_1.ll;
	$ll.addFirst($variable_declarations.ast);
  }
  | { $ll = new LinkedList<AstTree>(); }
  ;

type_declarations
  returns [AstTree ast]
  : TYP ID EQUAL type type_declarations_1 SEMICOLON
  {
	AstTypDecl n = new AstTypDecl(loc($ID, $type.t), $ID.text, $type.t);
	$type_declarations_1.ll.addFirst(n);
	$ast = new AstTrees<AstDecl>(loc($TYP, $SEMICOLON),
				     "type declarations",
				     $type_declarations_1.ll);
  }
  ;

type_declarations_1
  returns [LinkedList<AstDecl> ll]
  : COMMA ID EQUAL type type_declarations_1
  {
	$ll = $type_declarations_1.ll;
	$ll.addFirst(new AstTypDecl(loc($ID), $ID.text, $type.t));
  }
  | { $ll = new LinkedList<AstDecl>(); }
  ;

function_declarations
  returns [AstTree ast]
  : FUN ID LPAR function_params RPAR COLON type function_stmts
	function_declarations_1 SEMICOLON
  {
	AstFunDecl n = new AstFunDecl(loc($ID), $ID.text,
				      $function_params.ast, $type.t,
				      $function_stmts.stmt);
	LinkedList<AstDecl> ll = $function_declarations_1.ll;
	ll.addFirst(n);
	$ast = new AstTrees<AstDecl>(loc($FUN, $SEMICOLON),
				     "function declarations", ll);
  }
  ;

function_declarations_1
  returns [LinkedList<AstDecl> ll]
  : COMMA ID LPAR function_params RPAR COLON type function_stmts
	function_declarations_1
  {
	AstFunDecl n = new AstFunDecl(loc($ID), $ID.text,
				      $function_params.ast, $type.t,
				      $function_stmts.stmt);
	$ll = $function_declarations_1.ll;
	$ll.addFirst(n);
  }
  | { $ll = new LinkedList<AstDecl>(); }
  ;

function_params
  returns [AstTrees<AstParDecl> ast]
  : ID COLON type function_params_1
  {
	AstParDecl par = new AstParDecl(loc($ID, $type.t), $ID.text, $type.t);
	LinkedList<AstParDecl> ll = $function_params_1.ll;
	ll.addFirst(par);
	$ast = new AstTrees<AstParDecl>(loc($ID), "function params", ll);
  }
  | { $ast = new AstTrees<AstParDecl>("function params"); }
  ;

function_params_1
  returns [LinkedList<AstParDecl> ll]
  : COMMA ID COLON type function_params_1
  {
	AstParDecl par = new AstParDecl(loc($ID, $type.t), $ID.text, $type.t);
	$ll = $function_params_1.ll;
	$ll.addFirst(par);
  }
  | { $ll = new LinkedList<AstParDecl>(); }
  ;

function_stmts
  returns [AstStmt stmt]
  : EQUAL statement { $stmt = $statement.stmt; }
  | { $stmt = null; }
  ;

variable_declarations
  returns [AstTree ast]
  : VAR ID COLON type variable_declarations_1 SEMICOLON
  {
	AstVarDecl n = new AstVarDecl(loc($ID, $type.t), $ID.text, $type.t);
	$variable_declarations_1.ll.addFirst(n);
	$ast = new AstTrees<AstDecl>(loc($VAR, $SEMICOLON), "var declarations",
				     $variable_declarations_1.ll);
  }
  ;

variable_declarations_1
  returns [LinkedList<AstDecl> ll]
  : COMMA ID COLON type variable_declarations_1
  {
	$ll = $variable_declarations_1.ll;
	$ll.addFirst(new AstVarDecl(loc($ID), $ID.text, $type.t));
  }
  | { $ll = new LinkedList<AstDecl>(); }
  ;

type
  returns [AstType t]
  : VOID { $t = new AstAtomType(loc($VOID), AstAtomType.Type.VOID); }
  | CHAR { $t = new AstAtomType(loc($CHAR), AstAtomType.Type.CHAR); }
  | INT  { $t = new AstAtomType(loc($INT), AstAtomType.Type.INT); }
  | BOOL { $t = new AstAtomType(loc($BOOL), AstAtomType.Type.BOOL); }
  | ID   { $t = new AstNameType(loc($ID), $ID.text); }
  | LBRACKET expression RBRACKET type
  {
	$t = new AstArrType(loc($LBRACKET, $type.t), $type.t, $expression.ast);
  }
  | CARON type { $t = new AstPtrType(loc($CARON, $type.t), $type.t); }
  | LBRACE ID COLON type type_1 RBRACE
  {
	AstTrees<AstCmpDecl> comps;
	$type_1.ll.addFirst(new AstCmpDecl(loc($ID, $type.t), $ID.text, $type.t));
	comps = new AstTrees<AstCmpDecl>("component declaration", $type_1.ll);
	$t = new AstRecType(loc($LBRACE, $RBRACE), comps);
  }
  | LPAR type RPAR
  {
	$t = $type.t;
	$t.relocate(loc($LPAR, $RPAR));
  }
  ;

type_1
  returns [LinkedList<AstCmpDecl> ll]
  : COMMA ID COLON type type_1
  {
	$ll = $type_1.ll;
	$ll.addFirst(new AstCmpDecl(loc($ID, $type.t), $ID.text, $type.t));
  }
  | { $ll = new LinkedList<AstCmpDecl>(); }
  ;

// relational operators - non-associative
// other operators - left associative

disjunctive_operator
  returns [AstBinExpr.Oper oper]
  : PIPE { $oper = AstBinExpr.Oper.OR; }
  ;

conjunctive_operator
  returns [AstBinExpr.Oper oper]
  : AMPERSAND { $oper = AstBinExpr.Oper.AND; }
  ;

relational_operator
  returns [AstBinExpr.Oper oper]
  : EQ { $oper = AstBinExpr.Oper.EQU; }
  | NE { $oper = AstBinExpr.Oper.NEQ; }
  | LT { $oper = AstBinExpr.Oper.LTH; }
  | GT { $oper = AstBinExpr.Oper.GTH; }
  | LE { $oper = AstBinExpr.Oper.LEQ; }
  | GE { $oper = AstBinExpr.Oper.GEQ; }
  ;

additive_operator
  returns [AstBinExpr.Oper oper]
  : PLUS { $oper = AstBinExpr.Oper.ADD; }
  | MINUS { $oper = AstBinExpr.Oper.SUB; }
  ;

multiplicative_operator
  returns [AstBinExpr.Oper oper]
  : ASTERISK { $oper = AstBinExpr.Oper.MUL; }
  | SLASH { $oper = AstBinExpr.Oper.DIV; }
  | PERCENT { $oper = AstBinExpr.Oper.MOD; }
  ;

prefix_operator
  returns [AstPfxExpr.Oper oper]
  : EXCLAM { $oper = AstPfxExpr.Oper.NOT; }
  | PLUS { $oper = AstPfxExpr.Oper.ADD; }
  | MINUS { $oper = AstPfxExpr.Oper.SUB; }
  | CARON { $oper = AstPfxExpr.Oper.PTR; }
  ;

postfix_operator
  [AstExpr left] returns [AstExpr ast]
  : LBRACKET expression RBRACKET
  {
	$ast = new AstArrExpr(loc($LBRACKET, $RBRACKET), $left,
			       $expression.ast);
	$left = $ast;
  }
  | CARON
  {
	$ast = new AstSfxExpr(loc($CARON), AstSfxExpr.Oper.PTR, $left);
	$left = $ast;
  }
  | DOT ID
  {
	AstNameExpr component = new AstNameExpr(loc($ID), $ID.text);
	$ast = new AstRecExpr(loc($DOT, $ID), $left, component);
	$left = $ast;
  }
  ;

expression
  returns [AstExpr ast]
  : disjunctive_expression { $ast = $disjunctive_expression.ast; }
  ;

disjunctive_expression
  returns [AstExpr ast]
  : conjunctive_expression
  disjunctive_expression_1[$conjunctive_expression.ast]
  {
	$ast = $disjunctive_expression_1.ast;
  }
  ;

disjunctive_expression_1
  [AstExpr left] returns [AstExpr ast]
  : disjunctive_operator conjunctive_expression {
	AstExpr n = new AstBinExpr(loc($left, $conjunctive_expression.ast),
			      $disjunctive_operator.oper, $left,
			      $conjunctive_expression.ast);
  } disjunctive_expression_1[n] {
	$ast = $disjunctive_expression_1.ast;
  }
  | { $ast = $left; }
  ;

conjunctive_expression
  returns [AstExpr ast]
  : relational_expression conjunctive_expression_1[$relational_expression.ast]
  {
	$ast = $conjunctive_expression_1.ast;
  }
  ;

conjunctive_expression_1
  [AstExpr left] returns [AstExpr ast]
  : conjunctive_operator relational_expression {
	AstExpr n = new AstBinExpr(loc($left, $relational_expression.ast),
				   $conjunctive_operator.oper, $left,
				   $relational_expression.ast);
  } conjunctive_expression_1[n] {
	$ast = $conjunctive_expression_1.ast;
  }
  | { $ast = $left; }
  ;

// different associativity than others
relational_expression
  returns [AstExpr ast]
  : additive_expression relational_expression_1[$additive_expression.ast]
  {
	$ast = $relational_expression_1.ast;
  }
  ;

relational_expression_1
  [AstExpr left] returns [AstExpr ast]
  : relational_operator additive_expression {
	$ast = new AstBinExpr(loc($left, $additive_expression.ast),
			      $relational_operator.oper, $left,
			      $additive_expression.ast);
  }
  | { $ast = $left; }
  ;

additive_expression
  returns [AstExpr ast]
  : multiplicative_expression
  additive_expression_1[$multiplicative_expression.ast]
  {
	$ast = $additive_expression_1.ast;
  }
  ;

additive_expression_1
  [AstExpr left] returns [AstExpr ast]
  : additive_operator multiplicative_expression {
	AstExpr n = new AstBinExpr(loc($left, $multiplicative_expression.ast),
				   $additive_operator.oper, $left,
				   $multiplicative_expression.ast);
  } additive_expression_1[n] {
	$ast = $additive_expression_1.ast;
  }
  | { $ast = $left; }
  ;

multiplicative_expression
  returns [AstExpr ast]
  : prefix_expression multiplicative_expression_1[$prefix_expression.ast]
  {
	$ast = $multiplicative_expression_1.ast;
  }
  ;

multiplicative_expression_1
  [AstExpr left] returns [AstExpr ast]
  : multiplicative_operator prefix_expression {
	AstExpr n = new AstBinExpr(loc($left, $prefix_expression.ast),
			      $multiplicative_operator.oper, $left,
			      $prefix_expression.ast);
  } multiplicative_expression_1[n] {
	$ast = $multiplicative_expression_1.ast;
  }
  | { $ast = $left; }
  ;

prefix_expression
  returns [AstExpr ast]
  : prefix_operator prefix_expression
  {
	$ast = new AstPfxExpr(loc($prefix_expression.ast),
			      $prefix_operator.oper, $prefix_expression.ast);
  }
  | postfix_expression { $ast = $postfix_expression.ast; }
  ;

postfix_expression
  returns [AstExpr ast]
  : final_expression postfix_expression_1[$final_expression.ast]
  {
	$ast = $postfix_expression_1.ast;
  }
  ;

postfix_expression_1
  [AstExpr left] returns [AstExpr ast]
  : postfix_operator[$left] postfix_expression_1[$postfix_operator.ast]
  {
	$ast = $postfix_expression_1.ast;
  }
  | { $ast = $left; }
  ;

final_expression
  returns [AstExpr ast]
  : VOID_CONST
  {
	$ast = new AstAtomExpr(loc($VOID_CONST), AstAtomExpr.Type.VOID,
			       $VOID_CONST.text);
  }
  | BOOL_CONST
  {
	$ast = new AstAtomExpr(loc($BOOL_CONST), AstAtomExpr.Type.BOOL,
			       $BOOL_CONST.text);
  }
  | INT_CONST
  {
	$ast = new AstAtomExpr(loc($INT_CONST), AstAtomExpr.Type.INT,
			       $INT_CONST.text);
  }
  | CHAR_CONST
  {
	$ast = new AstAtomExpr(loc($CHAR_CONST), AstAtomExpr.Type.CHAR,
			       $CHAR_CONST.text);
  }
  | STRING_CONST
  {
	$ast = new AstAtomExpr(loc($STRING_CONST), AstAtomExpr.Type.STR,
			       $STRING_CONST.text);
  }
  | PTR_CONST
  {
	$ast = new AstAtomExpr(loc($PTR_CONST), AstAtomExpr.Type.PTR,
			       $PTR_CONST.text);
  }
  | ID final_expression_1[loc($ID), $ID.text]
  {
	$ast = $final_expression_1.ast;
  }
  | LPAR expression final_expression_4[$expression.ast] RPAR
  {
	$ast = $final_expression_4.ast;
  }
  | NEW LPAR type RPAR
  {
	$ast = new AstNewExpr(loc($NEW, $RPAR), $type.t);
  }
  | DEL LPAR expression RPAR
  {
	$ast = new AstDelExpr(loc($DEL, $RPAR), $expression.ast);
  }
  ;

final_expression_1
  [Location loc, String id] returns [AstExpr ast]
  : LPAR final_expression_2 RPAR
  {
	AstTrees<AstExpr> params = $final_expression_2.ast;
	$ast = new AstCallExpr(loc($LPAR, $RPAR), id, params);
  }
  | { $ast = new AstNameExpr(loc, id); }
  ;

final_expression_2
  returns [AstTrees<AstExpr> ast]
  : expression final_expression_3
  {
	LinkedList<AstExpr> ll = $final_expression_3.ll;
	ll.addFirst($expression.ast);
	$ast = new AstTrees<AstExpr>("args", ll);
  }
  | { $ast = new AstTrees<AstExpr>("args"); }
  ;

final_expression_3
  returns [LinkedList<AstExpr> ll]
  : COMMA expression final_expression_3
  {
	$ll = $final_expression_3.ll;
	$ll.addFirst($expression.ast);
  }
  | { $ll = new LinkedList<AstExpr>(); }
  ;

final_expression_4
  [AstExpr expr] returns [AstExpr ast]
  : COLON type
  {
	$ast = new AstCastExpr(loc($expr, $type.t), $expr, $type.t);
  }
  | { $ast = $expr; }
  ;

statement
  returns [AstStmt stmt]
  : expression statement_assign[$expression.ast]
  {
	$stmt = $statement_assign.stmt;
  }
  | IF expression THEN statement statement_else
  {
	Location loc = loc($IF, $statement_else.stmt);
	$stmt = new AstIfStmt(loc, $expression.ast, $statement.stmt,
			      $statement_else.stmt);
  }
  | WHILE expression DO statement
  {
	Location loc = loc($WHILE, $statement.stmt);
	$stmt = new AstWhileStmt(loc, $expression.ast, $statement.stmt);
  }
  | LET declarations IN statement
  {
	Location loc = loc($LET, $statement.stmt);
	AstTrees<AstTrees<AstDecl>> decls;
	decls = (AstTrees<AstTrees<AstDecl>>) $declarations.ast;
	$stmt = new AstDeclStmt(loc, decls, $statement.stmt);
  }
  | LBRACE statement statement_1 RBRACE
  {
	Location loc = loc($LBRACE, $RBRACE);
	LinkedList<AstStmt> ll = $statement_1.ll;
	ll.addFirst($statement.stmt);
	AstTrees<AstStmt> ast = new AstTrees<AstStmt>(loc, "statements", ll);
	$stmt = new AstStmts(loc($LBRACE, $RBRACE), ast);
  }
  ;

statement_assign
  [AstExpr expr] returns [AstStmt stmt]
  : EQUAL expression
  {
	$stmt = new AstAssignStmt(loc($expr, $expression.ast), $expr,
				  $expression.ast);
  }
  |
  {
	$stmt = new AstExprStmt(loc($expr), $expr);
  }
  ;

statement_else
  returns [AstStmt stmt]
  : ELSE statement
  {
	$stmt = $statement.stmt;
  }
  | { $stmt = null; }
  ;

statement_1
  returns [LinkedList<AstStmt> ll]
  : SEMICOLON statement statement_1
  {
	$ll = $statement_1.ll;
	$ll.addFirst($statement.stmt);
  }
  | { $ll = new LinkedList<AstStmt>(); }
  ;
