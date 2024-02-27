package prev23.phase.seman;

import prev23.common.report.*;
import prev23.data.ast.tree.*;
import prev23.data.ast.tree.decl.*;
import prev23.data.ast.tree.expr.*;
import prev23.data.ast.tree.stmt.*;
import prev23.data.ast.tree.type.*;
import prev23.data.ast.visitor.*;
import prev23.data.ast.attribute.*;
import prev23.data.typ.*;

import java.util.LinkedList;
import java.util.HashMap;

/**
 * TypeResolver resolver.
 * SemAn.declaredAt (AstName, AstNameDecl)
 * SemAn.declaresType (AstTypDecl, SemName)
 * SemAn.isType (AstType, SemType)
 * SemAn.ofType (AstExec, SemType)
 */
public class TypeResolver extends AstFullVisitor<SemType, Integer> {

	private boolean isVoid(AstType type) {
		if (type instanceof AstAtomType)
			if (((AstAtomType) type).type == AstAtomType.Type.VOID)
				return true;
		return false;
	}

	private SemType getActualType(SemType type) {
		if (type instanceof SemName)
			return getActualType(((SemName) type).type());
		return type;
	}

	private LinkedList<SemType> visited = new LinkedList<SemType>();

	private boolean equalType(SemType type1, SemType type2) {

		if (type1 == null) Report.warning("type1 is null");
		if (type2 == null) Report.warning("type2 is null");
		SemType t1 = getActualType(type1);
		SemType t2 = getActualType(type2);

		if (t1 instanceof SemVoid && t2 instanceof SemVoid) {
			visited.clear();
			return true;
		} else if (t1 instanceof SemChar && t2 instanceof SemChar) {
			visited.clear();
			return true;
		} else if (t1 instanceof SemInt && t2 instanceof SemInt) {
			visited.clear();
			return true;
		} else if (t1 instanceof SemBool && t2 instanceof SemBool) {
			visited.clear();
			return true;
		} else if (t1 instanceof SemArr && t2 instanceof SemArr) {
			SemArr a1 = (SemArr) t1;
			SemArr a2 = (SemArr) t2;
			if (visited.contains(a1) || visited.contains(a2)) {
				visited.clear();
				return true;
			}
			visited.add(a1);
			visited.add(a2);
			return equalType(a1.elemType, a2.elemType);
		} else if (t1 instanceof SemPtr && t2 instanceof SemPtr) {
			SemPtr p1 = (SemPtr) t1;
			SemPtr p2 = (SemPtr) t2;
			if (visited.contains(p1) || visited.contains(p2)) {
				visited.clear();
				return true;
			}
			visited.add(p1);
			visited.add(p2);
			return equalType(p1.baseType, p2.baseType);
		} else if (t1 instanceof SemRec && t2 instanceof SemRec) {
			SemRec r1 = (SemRec) t1;
			SemRec r2 = (SemRec) t2;

			int n1 = r1.numCmps();
			int n2 = r2.numCmps();
			if (n1 != n2) return false;
			for (int i = 0; i < n1; i++) {
				SemType c1 = getActualType(r1.cmpType(i));
				SemType c2 = getActualType(r2.cmpType(i));
				if (visited.contains(c1) ||
				    visited.contains(c2)) {
					visited.clear();
					return true;
				}
				visited.add(c1);
				visited.add(c2);
				if (!equalType(c1, c2)) return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public SemType visit(AstAtomType type, Integer arg) {
		SemType sType = null;

		// T1
		if (type.type == AstAtomType.Type.VOID)
			sType = new SemVoid();
		else if (type.type == AstAtomType.Type.CHAR)
			sType = new SemChar();
		else if (type.type == AstAtomType.Type.INT)
			sType = new SemInt();
		else if (type.type == AstAtomType.Type.BOOL)
			sType = new SemBool();

		if (SemAn.isType.get(type) == null)
			SemAn.isType.put(type, sType);
		else
			sType = SemAn.isType.get(type);
		return sType;
	}

	@Override
	public SemType visit(AstArrType type, Integer arg) {
		SemType sType = null;

		// T2
		// implicit rule definition !(n < 0)
		if (!(type.numElems instanceof AstAtomExpr))
			throw new Report.Error(type.numElems, "must be (unsigned) INT");
		if (((AstAtomExpr) type.numElems).type != AstAtomExpr.Type.INT)
			throw new Report.Error(type.numElems, "must be (unsigned) INT");

		AstAtomExpr e = (AstAtomExpr) type.numElems;

		long n = -1;
		// implicit rule definition !(n > 2^63-1)
		try {
			n = Long.parseLong(e.value);
		} catch (Exception ex) {
			throw new Report.Error(e, "INT out of range");
		}

		AstType t = type.elemType;
		if (isVoid(t))
			throw new Report.Error(t, "VOID not allowed");

		SemType elemType = null;
		if (type.elemType != null)
			elemType = getActualType(type.elemType.accept(this, arg));
		if (type.numElems != null)
			type.numElems.accept(this, arg);

		sType = new SemArr(elemType, n);

		if (SemAn.isType.get(type) == null)
			SemAn.isType.put(type, sType);
		else
			sType = SemAn.isType.get(type);
		return sType;
	}

	@Override
	public SemType visit(AstPtrType type, Integer arg) {
		SemType sType = null;

		// T4
		if (type.baseType != null)
			sType = new SemPtr(type.baseType.accept(this, arg));

		if (arg >= 2)
			SemAn.isType.put(type, sType);
		return sType;
	}

	@Override
	public SemType visit(AstAtomExpr expr, Integer arg) {
		SemType sType = null;
		if (expr.type == null) return sType;
		if (expr.value == null) return sType;

		// V1
		if (expr.value.equals("none"))
			sType = new SemVoid();
		else if (expr.value.equals("nil"))
			sType = new SemPtr(new SemVoid());
		else if (expr.type == AstAtomExpr.Type.STR)
			sType = new SemPtr(new SemChar());

		// V2
		if (expr.type == AstAtomExpr.Type.BOOL)
			sType = new SemBool();
		else if (expr.type == AstAtomExpr.Type.CHAR)
			sType = new SemChar();
		else if (expr.type == AstAtomExpr.Type.INT)
			sType = new SemInt();

		if (arg == 3)
			SemAn.ofType.put(expr, sType);
		return sType;
	}

	@Override
	public SemType visit(AstPfxExpr expr, Integer arg) {
		SemType sType = null;
		if (expr.oper == null) return sType;
		if (expr.expr == null) return sType;

		sType = expr.expr.accept(this, arg);
		
		// V3
		AstPfxExpr.Oper not = AstPfxExpr.Oper.NOT;
		AstPfxExpr.Oper add = AstPfxExpr.Oper.ADD;
		AstPfxExpr.Oper sub = AstPfxExpr.Oper.SUB;
		if (expr.oper == not && !(sType instanceof SemBool))
			throw new Report.Error(expr, "only BOOL can follow !");
		if (expr.oper == add && !(sType instanceof SemInt))
			throw new Report.Error(expr, "only INT can follow +");
		if (expr.oper == sub && !(sType instanceof SemInt))
			throw new Report.Error(expr, "only INT can follow -");

		// V8 (first part)
		AstPfxExpr.Oper ptr = AstPfxExpr.Oper.PTR;
		if (expr.oper == ptr)
			sType = new SemPtr(sType);

		if (arg == 3)
			SemAn.ofType.put(expr, sType);
		return sType;
	}

	@Override
	public SemType visit(AstBinExpr expr, Integer arg) {
		SemType sType = null;
		SemType fstType = null;
		SemType sndType = null;
		AstExpr fst = expr.fstExpr;
		AstExpr snd = expr.sndExpr;
		if (fst != null)
			fstType = getActualType(fst.accept(this, arg));
		if (snd != null)
			sndType = getActualType(snd.accept(this, arg));

		// V4
		AstBinExpr.Oper or = AstBinExpr.Oper.OR;
		AstBinExpr.Oper and = AstBinExpr.Oper.AND;
		if (expr.oper == or || expr.oper == and) {
			if (!(fstType instanceof SemBool))
				throw new Report.Error(fst, "BOOL required");
			if (!(sndType instanceof SemBool))
				throw new Report.Error(snd, "BOOL required");
			sType = new SemBool();
		}

		// V5
		AstBinExpr.Oper add = AstBinExpr.Oper.ADD;
		AstBinExpr.Oper sub = AstBinExpr.Oper.SUB;
		AstBinExpr.Oper mul = AstBinExpr.Oper.MUL;
		AstBinExpr.Oper div = AstBinExpr.Oper.DIV;
		AstBinExpr.Oper mod = AstBinExpr.Oper.MOD;
		if (expr.oper == add || expr.oper == sub || expr.oper == mul ||
		    expr.oper == div || expr.oper == mod) {
			if (fstType instanceof SemName)
				fstType = ((SemName) fstType).type();
			if (sndType instanceof SemName)
				sndType = ((SemName) sndType).type();

			if (!(fstType instanceof SemInt))
				throw new Report.Error(fst, "INT required");
			if (!(sndType instanceof SemInt))
				throw new Report.Error(snd, "INT required");

			sType = new SemInt();
			sType = new SemInt();
		}

		// V6
		AstBinExpr.Oper equ = AstBinExpr.Oper.EQU;
		AstBinExpr.Oper neq = AstBinExpr.Oper.NEQ;
		if (expr.oper == equ || expr.oper == neq) {
			if (!equalType(fstType, sndType))
				throw new Report.Error(expr, "Cannot compare different types");
			if (!(fstType instanceof SemBool ||
			      fstType instanceof SemChar ||
			      fstType instanceof SemInt ||
			      fstType instanceof SemPtr))
				throw new Report.Error(expr, "Must be BOOL, CHAR, INT, PTR");
			sType = new SemBool();
		}

		// V7
		AstBinExpr.Oper leq = AstBinExpr.Oper.LEQ;
		AstBinExpr.Oper geq = AstBinExpr.Oper.GEQ;
		AstBinExpr.Oper lth = AstBinExpr.Oper.LTH;
		AstBinExpr.Oper gth = AstBinExpr.Oper.GTH;
		if (expr.oper == leq || expr.oper == geq ||
		    expr.oper == lth || expr.oper == gth) {
			if (!(equalType(fstType, sndType)))
				throw new Report.Error(expr, "Cannot compare different types");
			if (!(fstType instanceof SemChar ||
			      fstType instanceof SemInt ||
			      fstType instanceof SemPtr))
				throw new Report.Error(expr, "Must be CHAR, INT, PTR");
			sType = new SemBool();
		}

		if (arg == 3)
			SemAn.ofType.put(expr, sType);
		return sType;
	}

	@Override
	public SemType visit(AstSfxExpr expr, Integer arg) {
		SemType sType = null;
		if (expr.expr == null) return sType;
		if (expr.oper == null) return sType;

		// V8 (second part)
		sType = expr.expr.accept(this, arg);
		if (sType instanceof SemName)
			sType = ((SemName) sType).actualType();
		if (sType instanceof SemPtr)
			sType = ((SemPtr) sType).baseType;
		else
			throw new Report.Error(expr, "Cannot dereference");

		if (arg == 3)
			SemAn.ofType.put(expr, sType);
		return sType;
	}

	@Override
	public SemType visit(AstNewExpr expr, Integer arg) {
		SemType sType = null;
		if (expr.type == null) return sType;

		// V9 (first part)
		sType = expr.type.accept(this, arg);
		sType = new SemPtr(sType);

		if (arg == 3)
			SemAn.ofType.put(expr, sType);
		return sType;
	}

	@Override
	public SemType visit(AstDelExpr expr, Integer arg) {
		SemType sType = null;
		if (expr.expr == null) return sType;

		// V9 (second part)
		sType = expr.expr.accept(this, arg);
		if (sType instanceof SemPtr)
			sType = new SemVoid();
		else
			throw new Report.Error(expr, "Must be pointer");

		if (arg == 3)
			SemAn.ofType.put(expr, sType);
		return sType;
	}

	@Override
	public SemType visit(AstArrExpr expr, Integer arg) {
		SemType sType = null;
		if (expr.arr == null) return sType;
		if (expr.idx == null) return sType;

		// V10
		sType = getActualType(expr.idx.accept(this, arg));
		if (!(sType instanceof SemInt))
			throw new Report.Error(expr.idx, "Must be INT");
		sType = getActualType(expr.arr.accept(this, arg));
		if (sType instanceof SemArr)
			sType = ((SemArr) sType).elemType;
		else
			throw new Report.Error(expr.arr, "Must be array type");

		if (arg == 3)
			SemAn.ofType.put(expr, sType);
		return sType;
	}

	public HashMap<SemType, SymbTable> recSymTabs = new HashMap<SemType, SymbTable>();

	private void redeclared_err(String name, Location loc) {
		String msg = "Redeclaration of record name \"" + name + "\"";
		throw new Report.Error(loc, msg);
	}

	private void undeclared_err(String name, Location loc) {
		String msg = "Undeclared record name \"" + name + "\"";
		throw new Report.Error(loc, msg);
	}

	@Override
	public SemType visit(AstRecType type, Integer arg) {
		SemType sType = null;
		if (type.comps == null) return sType;

		// T3 (outer part)
		LinkedList<SemType> ll = new LinkedList<SemType>();
		SymbTable st = new SymbTable();
		for (int i = 0; i < type.comps.size(); i++) {
			AstCmpDecl decl = type.comps.get(i);
			if (decl == null) continue;
			try {
				if (arg == 2)
					st.ins(decl.name, decl);
			} catch (Exception e) {
				redeclared_err(decl.name, decl.location());
			}
			if (decl.type == null) continue;
			AstType t = decl.type;
			if (isVoid(t))
				throw new Report.Error(t, "VOID not allowed");
			sType = decl.type.accept(this, arg);
			if (arg == 2)
				SemAn.isType.put(decl.type, sType);
			ll.add(sType);
		}

		if (arg == 2) {
			sType = new SemRec(ll);
			SemAn.isType.put(type, sType);
			recSymTabs.put(sType, st);
		} else if (arg == 3) {
			sType = SemAn.isType.get(type);
		}
		return sType;
	}

	@Override
	public SemType visit(AstRecExpr expr, Integer arg) {
		SemType sType = null;
		if (expr.rec == null) return sType;
		if (expr.comp == null) return sType;

		// V11 (outer part)
		sType = expr.rec.accept(this, arg);
		try {
			SemType t = getActualType(sType).actualType();
			SymbTable st = recSymTabs.get(t);
			AstCmpDecl comp = (AstCmpDecl) st.fnd(expr.comp.name);
			SemAn.declaredAt.put(expr.comp, comp);
			sType = comp.accept(this, arg);
		} catch (Exception e) {
			AstNameExpr ne = (AstNameExpr) expr.rec;
			String msg = ne.name + "." + expr.comp.name;
			undeclared_err(msg, expr.location());
		}

		if (arg == 3)
			SemAn.ofType.put(expr, sType);
		return sType;
	}

	@Override
	public SemType visit(AstCmpDecl decl, Integer arg) {
		SemType sType = null;
		if (decl.type == null) return sType;

		// V11 (inner part)
		sType = decl.type.accept(this, arg);

		if (arg == 3)
			SemAn.isType.put(decl.type, sType); // ne izrise nic
		return sType;
	}

	@Override
	public SemType visit(AstCallExpr expr, Integer arg) {
		SemType sType = null;
		if (arg <= 2) throw new Report.Error("how am i here?");

		// V12 (outer part)
		if (expr.args != null)
			expr.args.accept(this, arg);

		AstNameDecl nDecl = SemAn.declaredAt.get(expr);
		if (!(nDecl instanceof AstFunDecl))
			throw new Report.Error("Calling parameter instead function");
		AstFunDecl fDecl = (AstFunDecl) nDecl;

		sType = fDecl.type.accept(this, arg);

		// Check arguments against the definition
		int nd = fDecl.pars.size();
		int ne = -1;
		if (expr.args != null)
			ne = expr.args.size();
		if (nd != ne)
			throw new Report.Error(expr, "Number of provided arguments does not match the expected amount");
		for (int i = 0; i < nd; i++) {
			AstParDecl par = fDecl.pars.get(i);
			AstExpr _arg = expr.args.get(i);
			SemType parType = SemAn.isType.get(par.type);
			SemType argType = SemAn.ofType.get(_arg);
			if (parType == null)
				parType = par.type.accept(this, arg);
			if (argType == null)
				argType = _arg.accept(this, arg);
			if (!equalType(parType, argType))
				throw new Report.Error(_arg, "Argument type does not match the expected type");
		}

		if (arg == 3)
			SemAn.ofType.put(expr, sType);
		return sType;
	}

	@Override
	public SemType visit(AstCastExpr expr, Integer arg) {
		SemType sType = null;
		if (expr.expr == null) return sType;
		if (expr.type == null) return sType;

		// V13
		SemType eType = getActualType(expr.expr.accept(this, arg));
		SemType tType = getActualType(expr.type.accept(this, arg));
		if (!(eType instanceof SemChar || eType instanceof SemInt ||
		    eType instanceof SemPtr))
			throw new Report.Error(expr.expr, "Cannot cast from");
		if (!(tType instanceof SemChar || tType instanceof SemInt ||
		    tType instanceof SemPtr))
			throw new Report.Error(expr.type, "Cannot cast to");
		sType = tType;

		SemAn.ofType.put(expr, sType);
		return sType;
	}

	@Override
	public SemType visit(AstAssignStmt stmt, Integer arg) {
		SemType sType = null;
		if (stmt.dst == null) return sType;
		if (stmt.src == null) return sType;

		// S1
		SemType dstType = stmt.dst.accept(this, arg);
		SemType srcType = stmt.src.accept(this, arg);
		if (!(equalType(dstType, srcType)))
			throw new Report.Error(stmt, "Cannot assign a different type");
		if (!(dstType instanceof SemBool ||
		    dstType instanceof SemChar ||
		    dstType instanceof SemInt ||
		    dstType instanceof SemPtr ||
		    dstType instanceof SemName)) 
			throw new Report.Error(stmt, "Must be CHAR, INT, PTR");

		sType = new SemVoid();

		SemAn.ofType.put(stmt, sType);
		return sType;
	}

	@Override
	public SemType visit(AstNameExpr expr, Integer arg) {
		SemType sType = null;
		if (arg < 2) return null;

		AstNameDecl nDecl = SemAn.declaredAt.get(expr);
		if (nDecl instanceof AstVarDecl) {
			AstVarDecl vDecl = (AstVarDecl) nDecl;
			sType = SemAn.isType.get(vDecl.type);
			if (sType == null)
				sType = vDecl.type.accept(this, arg);
		} else if (nDecl instanceof AstParDecl) {
			AstParDecl pDecl = (AstParDecl) nDecl;
			sType = SemAn.isType.get(pDecl.type);
			if (sType == null)
				sType = pDecl.type.accept(this, arg);
		} else if (nDecl instanceof AstCmpDecl) {
			AstCmpDecl cDecl = (AstCmpDecl) nDecl;
			sType = SemAn.isType.get(cDecl.type);
			if (sType == null)
				sType = cDecl.type.accept(this, arg);
		} else {
			throw new Report.Error(expr, "Must be variable ID");
		}
		SemAn.ofType.put(expr, sType);
		return sType;
	}

	@Override
	public SemType visit(AstIfStmt stmt, Integer arg) {
		SemType sType = null;
		if (stmt.cond == null) return sType;
		if (stmt.thenStmt == null) return sType;

		// S2
		SemType condType = stmt.cond.accept(this, arg);
		SemType thenType = stmt.thenStmt.accept(this, arg);
		if (!(condType instanceof SemBool))
			throw new Report.Error(stmt.cond, "Must be BOOL");

		// S3
		if (stmt.elseStmt != null)
			stmt.elseStmt.accept(this, arg);

		sType = new SemVoid();
		SemAn.ofType.put(stmt, sType);
		return sType;
	}

	@Override
	public SemType visit(AstWhileStmt stmt, Integer arg) {
		SemType sType = null;
		if (stmt.cond == null) return sType;
		if (stmt.bodyStmt == null) return sType;

		// S4
		SemType condType = stmt.cond.accept(this, arg);
		SemType bodyType = stmt.bodyStmt.accept(this, arg);
		if (!(condType instanceof SemBool))
			throw new Report.Error(stmt.cond, "Must be BOOL");

		sType = new SemVoid();
		SemAn.ofType.put(stmt, sType);
		return sType;
	}

	@Override
	public SemType visit(AstDeclStmt stmt, Integer arg) {
		SemType sType = null;
		if (stmt.stmt == null) return sType;
		if (stmt.decls == null) return sType;

		// S5
		stmt.decls.accept(this, arg);
		sType = stmt.stmt.accept(this, arg);

		SemAn.ofType.put(stmt, sType);
		return sType;
	}

	@Override
	public SemType visit(AstStmts stmts, Integer arg) {
		SemType sType = null;
		if (stmts.stmts == null) return sType;

		// S6 (outer part)
		sType = stmts.stmts.accept(this, arg);

		SemAn.ofType.put(stmts, sType);
		return sType;
	}

	@Override
	public SemType visit(AstTrees<? extends AstTree> tree, Integer arg) {
		SemType sType = null;

		// V12 (inner part)
		// S6 (inner part)
		// D3 (inner part)
		// D4 (inner part)
		for (int i = 0; i < tree.size(); i++) {
			AstTree t = tree.get(i);
			if (t == null) continue;
			String title = "";
			if (t instanceof AstTrees)
				title = ((AstTrees) t).title;
			if (title.equals("type declarations") ||
			    title.equals("function declarations") ||
			    title.equals("var declarations")) {
				t.accept(this, 1);
				t.accept(this, 2);
				t.accept(this, 3);
			} else {
				sType = t.accept(this, arg);
			}
		}

		return sType;
	}

	@Override
	public SemType visit(AstExprStmt stmt, Integer arg) {
		SemType sType = null;
		if (stmt.expr == null) return sType;

		// V12 (inner inner part)
		// S6 (inner inner part)
		sType = stmt.expr.accept(this, arg);

		SemAn.ofType.put(stmt, sType);
		return sType;
	}

	@Override
	public SemType visit(AstTypDecl decl, Integer arg) {
		SemType sType = null;
		if (decl.type == null) return sType;

		// D1
		sType = decl.type.accept(this, arg);
		if (arg == 1) {
			SemName sName = new SemName(decl.name);
			SemAn.declaresType.put(decl, sName);
		} else if (arg == 2) {
			SemName sName = SemAn.declaresType.get(decl);
			sName.define(sType);
			SemAn.isType.put(decl.type, sType);
		}

		return sType;
	}

	private SemType maxDereference(SemType s) {
		SemType sType = getActualType(s);
		if (sType instanceof SemPtr)
			return maxDereference(((SemPtr) sType).baseType);
		else if (sType instanceof SemArr)
			return maxDereference(((SemArr) sType).elemType);
		return sType;
	}

	@Override
	public SemType visit(AstVarDecl decl, Integer arg) {
		SemType sType = null;
		if (arg < 2) return null;
		if (decl.type == null) return sType;

		// D2 (outer part)
		sType = decl.type.accept(this, arg);
		if (sType instanceof SemVoid)
			throw new Report.Error(decl, "var type can't be void");

		// fix for "var a : (^)*a;"
		SemType tmp = sType;
		tmp = maxDereference(tmp);
		if (tmp == null)
			throw new Report.Error(decl.type, "Not a type");

		SemAn.isType.put(decl.type, sType);
		return sType;
	}

        @Override
        public SemType visit(AstNameType type, Integer arg) {
		SemType sType = null;
		AstNameDecl decl = SemAn.declaredAt.get(type);

		if (decl instanceof AstTypDecl) {
			// D2 (inner part)
			AstTypDecl tDecl = (AstTypDecl) decl;
			sType = SemAn.declaresType.get(tDecl);
		}

		if (arg >= 2)
			SemAn.isType.put(type, sType);
		return sType;
	}

	@Override
	public SemType visit(AstFunDecl decl, Integer arg) {
		SemType sType = null;
		if (arg <= 2) return null;
		if (decl.pars == null) return sType;
		if (decl.type == null) return sType;

		// D3 (outer part)
		decl.pars.accept(this, arg);
		sType = getActualType(decl.type.accept(this, arg));

		if (arg == 3 && decl.stmt != null) {

			// D4 (outer part)
			SemType stmtType = null;
			stmtType = getActualType(decl.stmt.accept(this, arg));
			if (!equalType(stmtType, sType))
				throw new Report.Error(decl, "Return type mismatch");
		}

		SemAn.isType.put(decl.type, sType);
		return sType;
	}

	@Override
	public SemType visit(AstParDecl decl, Integer arg) {
		SemType sType = null;
		if (decl.type == null) return sType;

		// D3 (inner inner part)
		// D4 (inner inner part)
		sType = getActualType(decl.type.accept(this, arg));
		if (!(sType instanceof SemBool || sType instanceof SemChar ||
		    sType instanceof SemInt || sType instanceof SemPtr))
			throw new Report.Error(decl, "Must be BOOL, CHAR, INT, PTR");
		SemAn.isType.put(decl.type, sType);
		return sType;
	}

}
// is a[-10] legal?

// 1st pass - names
// 2nd pass - record inner, variable type assign
// 3rd pass - function body
