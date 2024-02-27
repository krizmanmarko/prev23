package prev23.phase.seman;

import prev23.common.report.*;
import prev23.data.ast.attribute.*;
import prev23.data.ast.tree.*;
import prev23.data.ast.tree.decl.*;
import prev23.data.ast.tree.expr.*;
import prev23.data.ast.tree.stmt.*;
import prev23.data.ast.tree.type.*;
import prev23.data.ast.visitor.*;

/**
 * Name resolver.
 * 
 * Name resolver connects each node of a abstract syntax tree where a name is
 * used with the node where it is declared. The only exceptions are a record
 * field names which are connected with its declarations by type resolver. The
 * results of the name resolver are stored in
 * {@link prev23.phase.seman.SemAn#declaredAt}.
 */
public class NameResolver extends AstFullVisitor<Object, Integer> {

	private SymbTable symTab = new SymbTable();

	private void redeclared_err(String name, Location loc) {
		String msg = "Redeclaration of variable \"" + name + "\"";
		throw new Report.Error(loc, msg);
	}

	private void undeclared_err(String name, Location loc) {
		String msg = "Undeclared variable \"" + name + "\"";
		throw new Report.Error(loc, msg);
	}

	/*
	 * declarations, type declarations, fun declarations, params,
	 * statements, var declarations, args
	 */
	@Override
	public AstNode visit(AstTrees<? extends AstTree> tree, Integer arg) {
		for (int i = 0; i < tree.size(); i++) {
			AstTree t = tree.get(i);

			if (t == null) continue;

			String title = "";
			if (t instanceof AstTrees)
				title = ((AstTrees) t).title;

			// DECLARATIONS
			if (title.equals("type declarations")
			    || title.equals("function declarations")
			    || title.equals("var declarations")) {
				symTab.newScope();
				//System.out.println("Rule 1: new scope");
				t.accept(this, 1);
				//System.out.println("first pass complete");
				t.accept(this, 2);
				//System.out.println("second pass complete");
				t.accept(this, 3);
				//System.out.println("third pass complete");
			} else {
				t.accept(this, arg);
			}
		}
		return null;
	}

	@Override
	public AstNode visit(AstFunDecl decl, Integer arg) {
		if (arg == 1) {
			try {
				//System.out.printf("fun declaration: %s\n", decl.name);
				symTab.ins(decl.name, decl);
			} catch (Exception e) {
				redeclared_err(decl.name, decl.location());
			}
		} else if (arg == 2) {
			// type belongs to outer scope
			if (decl.type != null)
				decl.type.accept(this, arg);
		} else if (arg == 3) {
			symTab.newScope();
			//System.out.println("Rule 3: new scope");
			if (decl.pars != null)
				decl.pars.accept(this, arg);
			if (decl.stmt != null)
				decl.stmt.accept(this, arg);
			symTab.oldScope();
			//System.out.println("Rule 3: old scope");
		} else {
			throw new Report.Error("Unreachable reached");
		}
		return null;
	}

	@Override
	public AstNode visit(AstParDecl decl, Integer arg) {
		try {
			//System.out.printf("fun params: %s\n", decl.name);
			symTab.ins(decl.name, decl);
		} catch (Exception e) {
			redeclared_err(decl.name, decl.location());
		}
		if (decl.type != null)
			decl.type.accept(this, arg);
		return null;
	}

	@Override
	public AstNode visit(AstTypDecl decl, Integer arg) {
		if (arg == 1) {
			try {
				//System.out.printf("typ declaration: %s\n", decl.name);
				symTab.ins(decl.name, decl);
			} catch (Exception e) {
				redeclared_err(decl.name, decl.location());
			}
		}
		if (decl.type != null)
			decl.type.accept(this, arg);
		return null;
	}

	@Override
	public AstNode visit(AstVarDecl decl, Integer arg) {
		if (arg == 1) {
			try {
				//System.out.printf("var declaration: %s\n", decl.name);
				symTab.ins(decl.name, decl);
			} catch (Exception e) {
				redeclared_err(decl.name, decl.location());
			}
		}
		if (decl.type != null)
			decl.type.accept(this, arg);
		return null;
	}

	@Override
	public AstNode visit(AstCallExpr expr, Integer arg) {
		if (arg >= 3) {
		try {
			AstNameDecl decl = symTab.fnd(expr.name);
			//System.out.printf("usage (call): %s\n", expr.name);
			SemAn.declaredAt.put(expr, decl);
		} catch (Exception e) {
			undeclared_err(expr.name, expr.location());
		}
		}
		if (expr.args != null)
			expr.args.accept(this, arg);
		return null;
	}

	@Override
	public AstNode visit(AstNameExpr expr, Integer arg) {
		if (arg >= 2) {
			try {
				AstNameDecl decl = symTab.fnd(expr.name);
				//System.out.printf("usage (expr): %s\n", expr.name);
				SemAn.declaredAt.put(expr, decl);
			} catch (Exception e) {
				undeclared_err(expr.name, expr.location());
			}
		}
		return null;
	}

	@Override
	public AstNode visit(AstRecExpr expr, Integer arg) {
		if (expr.rec != null)
			expr.rec.accept(this, arg);
		return null;
	}

	@Override
	public AstNode visit(AstDeclStmt stmt, Integer arg) {
		symTab.newScope();
		//System.out.println("Rule 2: new scope");
		int extraScopes = -1;
		// used to deal with such case:
		// fun a(s : int) : int = let var b : int; in a;
	 	// fun main() : int = s; <-- s was happily defined
		if (stmt.decls != null) {
			stmt.decls.accept(this, arg);
			extraScopes = stmt.decls.size();
		}
		if (stmt.stmt != null)
			stmt.stmt.accept(this, arg);
		for (int i = 0; i < extraScopes; i++)
			symTab.oldScope();
		//System.out.println("deleting " + extraScopes + " extra scopes");
		symTab.oldScope();
		//System.out.println("Rule 2: old scope");
		return null;
	}

	@Override
	public AstNode visit(AstNameType type, Integer arg) {
		if (arg >= 2) {
			try {
				AstNameDecl decl = symTab.fnd(type.name);
				//System.out.printf("usage (type): %s\n", type.name);
				SemAn.declaredAt.put(type, decl);
			} catch (Exception e) {
				undeclared_err(type.name, type.location());
			}
		}
		return null;
	}
}
// 1st pass --> head
// 2nd pass --> body
// 3rd pass --> function body
