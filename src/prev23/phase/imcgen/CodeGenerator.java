package prev23.phase.imcgen;

import prev23.common.report.*;
import prev23.phase.seman.*;
import prev23.phase.memory.*;
import prev23.data.ast.attribute.*;
import prev23.data.ast.tree.*;
import prev23.data.ast.tree.decl.*;
import prev23.data.ast.tree.expr.*;
import prev23.data.ast.tree.stmt.*;
import prev23.data.ast.tree.type.*;
import prev23.data.ast.visitor.*;
import prev23.data.typ.*;
import prev23.data.mem.*;
import prev23.data.imc.code.*;
import prev23.data.imc.code.*;
import prev23.data.imc.code.expr.*;
import prev23.data.imc.code.stmt.*;
import java.util.Stack;
import java.util.Vector;

public class CodeGenerator extends AstFullVisitor<ImcInstr, Stack<MemFrame>> {

	@Override
	public ImcInstr visit(AstTrees<? extends AstTree> trees, Stack<MemFrame> arg) {
		if (arg == null)
			arg = new Stack<MemFrame>();
		for (AstTree t : trees)
			t.accept(this, arg);
		return null;
	}

	private ImcStmt saveRetVal(ImcStmt s, MemFrame frame) {
		ImcStmt st = null;
		ImcStmt ret = null;
		ImcTEMP retval = new ImcTEMP(frame.RV);
		if (s instanceof ImcSTMTS) {
			ImcSTMTS stmts = (ImcSTMTS) s;
			st = stmts.stmts.get(stmts.stmts.size() - 1);
			ImcExpr e = null;
			if (st instanceof ImcSTMTS) {
				ImcStmt last = st;
				ImcSTMTS prev = null;
				while (last instanceof ImcSTMTS) {
					ImcSTMTS tmp = (ImcSTMTS) last;
					int size = tmp.stmts.size();
					prev = tmp;
					last = tmp.stmts.get(size - 1);
				}
				ImcStmt tmp = saveRetVal(last, frame);
				prev.stmts.set(prev.stmts.size() - 1, tmp);
				return stmts;
			}
			else if (st instanceof ImcESTMT) {
				e = ((ImcESTMT) st).expr;
				ret = new ImcMOVE(retval, e);
				stmts.stmts.set(stmts.stmts.size() - 1, ret);
			} else {
				e = new ImcCONST(0xa5);
				ret = new ImcMOVE(retval, e);
				stmts.stmts.add(ret);
			}

			ret = stmts;
		} else if (s instanceof ImcESTMT) {
			st = s;
			ImcExpr e = null;

			e = ((ImcESTMT) st).expr;
			ret = new ImcMOVE(retval, e);
		} else {
			ImcExpr e = new ImcCONST(0xa5);
			Vector<ImcStmt> stmts = new Vector<ImcStmt>();
			stmts.add(s);
			stmts.add(new ImcMOVE(retval, e));
			ret = new ImcSTMTS(stmts);
		}
		return ret;
	}

	@Override
	public ImcInstr visit(AstFunDecl decl, Stack<MemFrame> arg) {
		MemFrame frame = Memory.frames.get(decl);
		arg.push(frame);

		ImcStmt s = null;
		if (decl.stmt != null) {
			s = (ImcStmt) decl.stmt.accept(this, arg);
			s = saveRetVal(s, frame);
			ImcGen.stmtImc.put(decl.stmt, s);
		}

		arg.pop();
		return null;
	}

	@Override
	public ImcInstr visit(AstAtomExpr expr, Stack<MemFrame> arg) {
		ImcExpr c = null;
		if (expr.type == null) return c;

		// EX1
		if (expr.type == AstAtomExpr.Type.VOID) {
			c = new ImcCONST(0xa5);
		} else if (expr.type == AstAtomExpr.Type.PTR) {
			if (expr.value.equals("nil")) {
				c = new ImcCONST(0);
			} else {
				Report.warning("not implemented yet");
			}
		// EX2
		} else if (expr.type == AstAtomExpr.Type.BOOL) {
			if (expr.value.equals("true"))
				c = new ImcCONST(1);
			else
				c = new ImcCONST(0);
		// EX3
		} else if (expr.type == AstAtomExpr.Type.CHAR) {
			String s = expr.value;
			long val = s.charAt(s.length() - 2);
			c = new ImcCONST(val);
		} else if (expr.type == AstAtomExpr.Type.INT) {
			long val = -1;
			try {
				val = Long.parseLong(expr.value);
			} catch (Exception e) {
				throw new Report.Error(expr, "INT out of range");
			}
			c = new ImcCONST(val);
		} else if (expr.type == AstAtomExpr.Type.STR) {
			MemLabel label = Memory.strings.get(expr).label;
			c = new ImcNAME(label);
			//long val = 0xa5;
			//c = new ImcCONST(val);
		}

		ImcGen.exprImc.put(expr, c);
		return c;
	}

	@Override
	public ImcInstr visit(AstNameExpr expr, Stack<MemFrame> arg) {
		ImcMEM mem = null;
		AstNameDecl n = SemAn.declaredAt.get(expr);
		if (n instanceof AstMemDecl) {
			// EX9
			MemAccess access = Memory.accesses.get((AstMemDecl) n);
			if (access instanceof MemAbsAccess) {
				MemLabel label = null;
				label = ((MemAbsAccess) access).label;
				ImcNAME name = new ImcNAME(label);
				mem = new ImcMEM(name);
			} else { // if (access instanceof MemRelAccess)
				MemRelAccess rel = (MemRelAccess) access;
				MemFrame frame = arg.peek();
				MemTemp fp = frame.FP;
				ImcExpr fpTemp = new ImcTEMP(fp);

				int diff = frame.depth - rel.depth;
				for (int i = 0; i < diff; i++) {
					fpTemp = new ImcMEM(fpTemp);
				}
				ImcCONST offset = new ImcCONST(rel.offset);
				mem = new ImcMEM(
					new ImcBINOP(
						ImcBINOP.Oper.ADD,
						fpTemp,
						offset
					)
				);
			}
		}

		ImcGen.exprImc.put(expr, mem);
		return mem;
	}

	@Override
	public ImcInstr visit(AstPfxExpr expr, Stack<MemFrame> arg) {
		ImcExpr unop = null;
		if (expr.expr == null) return unop;

		ImcExpr e = (ImcExpr) expr.expr.accept(this, arg);
		if (e == null) Report.warning(expr, "expression is null");

		// EX4
		if (expr.oper == AstPfxExpr.Oper.NOT)
			unop = new ImcUNOP(ImcUNOP.Oper.NOT, e);
		else if (expr.oper == AstPfxExpr.Oper.SUB)
			unop = new ImcUNOP(ImcUNOP.Oper.NEG, e);
		else if (expr.oper == AstPfxExpr.Oper.ADD)
			unop = e;
		// EX6
		else if (expr.oper == AstPfxExpr.Oper.PTR)
			unop = ((ImcMEM) e).addr;

		ImcGen.exprImc.put(expr, unop);
		return unop;
	}

	@Override
	public ImcInstr visit(AstSfxExpr expr, Stack<MemFrame> arg) {
		ImcExpr e = null;
		if (expr.oper == null) return e;

		e = (ImcExpr) expr.expr.accept(this, arg);
		// EX6
		ImcMEM mem = new ImcMEM(e);
		ImcGen.exprImc.put(expr, mem);
		return mem;
	}

	@Override
	public ImcInstr visit(AstBinExpr expr, Stack<MemFrame> arg) {
		ImcBINOP binop = null;
		if (expr.fstExpr == null) return binop;
		if (expr.sndExpr == null) return binop;

		ImcExpr fst = (ImcExpr) expr.fstExpr.accept(this, arg);
		ImcExpr snd = (ImcExpr) expr.sndExpr.accept(this, arg);
		if (fst == null) Report.warning("fst expression is null");
		if (snd == null) Report.warning("snd expression is null");

		// EX5
		if (expr.oper == AstBinExpr.Oper.OR)
			binop = new ImcBINOP(ImcBINOP.Oper.OR, fst, snd);
		else if (expr.oper == AstBinExpr.Oper.AND)
			binop = new ImcBINOP(ImcBINOP.Oper.AND, fst, snd);
		else if (expr.oper == AstBinExpr.Oper.EQU)
			binop = new ImcBINOP(ImcBINOP.Oper.EQU, fst, snd);
		else if (expr.oper == AstBinExpr.Oper.NEQ)
			binop = new ImcBINOP(ImcBINOP.Oper.NEQ, fst, snd);
		else if (expr.oper == AstBinExpr.Oper.LTH)
			binop = new ImcBINOP(ImcBINOP.Oper.LTH, fst, snd);
		else if (expr.oper == AstBinExpr.Oper.GTH)
			binop = new ImcBINOP(ImcBINOP.Oper.GTH, fst, snd);
		else if (expr.oper == AstBinExpr.Oper.LEQ)
			binop = new ImcBINOP(ImcBINOP.Oper.LEQ, fst, snd);
		else if (expr.oper == AstBinExpr.Oper.GEQ)
			binop = new ImcBINOP(ImcBINOP.Oper.GEQ, fst, snd);
		else if (expr.oper == AstBinExpr.Oper.ADD)
			binop = new ImcBINOP(ImcBINOP.Oper.ADD, fst, snd);
		else if (expr.oper == AstBinExpr.Oper.SUB)
			binop = new ImcBINOP(ImcBINOP.Oper.SUB, fst, snd);
		else if (expr.oper == AstBinExpr.Oper.MUL)
			binop = new ImcBINOP(ImcBINOP.Oper.MUL, fst, snd);
		else if (expr.oper == AstBinExpr.Oper.DIV)
			binop = new ImcBINOP(ImcBINOP.Oper.DIV, fst, snd);
		else if (expr.oper == AstBinExpr.Oper.MOD)
			binop = new ImcBINOP(ImcBINOP.Oper.MOD, fst, snd);

		ImcGen.exprImc.put(expr, binop);
		return binop;
	}

	@Override
	public ImcInstr visit(AstNewExpr expr, Stack<MemFrame> arg) {
		ImcExpr e = null;
		if (expr.type == null) return e;
		e = (ImcExpr) expr.type.accept(this, arg);

		// EX7 - may be incorrect (how to create a ptr??)
		// instead of size it should be value of ptr
		// which points to memory area of size "size"
		SemType sType = SemAn.ofType.get(expr);
		long size = -1;
		// new expression always returns SemPtr(type)
		SemPtr p = (SemPtr) sType;
		size = p.baseType.size();
		Vector<Long> offsets = new Vector<Long>();
		offsets.add((Long) 0L);
		offsets.add((Long) 8L);
		Vector<ImcExpr> args = new Vector<ImcExpr>();
		args.add(new ImcCONST(0xa5));
		args.add(new ImcCONST(size));
		e = new ImcCALL(
			new MemLabel("new"),
			offsets,
			args
		);

		ImcGen.exprImc.put(expr, e);
		return e;
	}

	@Override
	public ImcInstr visit(AstDelExpr expr, Stack<MemFrame> arg) {
		ImcExpr e = null;
		if (expr.expr == null) return e;
		e = (ImcExpr) expr.expr.accept(this, arg);

		// EX8
		Vector<Long> offsets = new Vector<Long>();
		offsets.add((Long) 0L);
		offsets.add((Long) 8L);
		Vector<ImcExpr> args = new Vector<ImcExpr>();
		args.add(new ImcCONST(0xa5));
		args.add(e);
		e = new ImcCALL(
			new MemLabel("del"),
			offsets,
			args
		);

		ImcGen.exprImc.put(expr, e);
		return e;
	}

	@Override
	public ImcInstr visit(AstArrExpr expr, Stack<MemFrame> arg) {
		ImcExpr e = null;
		if (expr.arr == null) return e;
		if (expr.idx == null) return e;
		ImcExpr arr = (ImcExpr) expr.arr.accept(this, arg);
		if (arr instanceof ImcMEM) // return addr not value
			arr = ((ImcMEM) arr).addr;
		ImcExpr idx = (ImcExpr) expr.idx.accept(this, arg);

		// EX10: arr + idx * size
		long elemSize = SemAn.ofType.get(expr).size();
		ImcBINOP offset = new ImcBINOP(
			ImcBINOP.Oper.MUL,
			idx,
			new ImcCONST(elemSize)
		);
		e = new ImcMEM(
			new ImcBINOP(
				ImcBINOP.Oper.ADD,
				arr,
				offset
			)
		);

		ImcGen.exprImc.put(expr, e);
		return e;
	}

        private SemType getActualType(SemType type) {
                if (type instanceof SemName)
                        return getActualType(((SemName) type).type());
                return type;
        }

	@Override
	public ImcInstr visit(AstRecExpr expr, Stack<MemFrame> arg) {
		ImcExpr e = null;
		if (expr.rec == null) return e;
		if (expr.comp == null) return e;
		ImcExpr rec = (ImcExpr) expr.rec.accept(this, arg);
		expr.comp.accept(this, arg);

		// EX11
		AstCmpDecl decl = (AstCmpDecl) SemAn.declaredAt.get(expr.comp);
		MemRelAccess access = (MemRelAccess) Memory.accesses.get(decl);

		// rec + identifier offset
		ImcBINOP addr = new ImcBINOP(
			ImcBINOP.Oper.ADD,
			rec,
			new ImcCONST(access.offset)
		);
		e = new ImcMEM(addr);

		ImcGen.exprImc.put(expr, e);
		return e;
	}

	@Override
	public ImcInstr visit(AstCallExpr expr, Stack<MemFrame> arg) {
		ImcExpr e = null;
		if (expr.args != null)
			expr.args.accept(this, arg);

		// EX12
		AstFunDecl decl = (AstFunDecl) SemAn.declaredAt.get(expr);
		MemFrame frame = Memory.frames.get(decl);
		MemFrame curr = arg.peek();

		ImcExpr sl = null;
		if (frame.depth == 0) {
			sl = new ImcCONST(0xa5);
		} else {
			sl = new ImcTEMP(curr.FP);
			int diff = curr.depth - frame.depth + 1;
			for (int i = 0; i < diff; i++) {
				sl = new ImcMEM(sl);
			}
		}

		Vector<Long> offsets = new Vector<Long>();
		offsets.add(0L); // SL
		Vector<ImcExpr> args = new Vector<ImcExpr>();
		args.add(sl); // SL

		for (int i = 0; i < expr.args.size(); i++) {
			AstParDecl par = decl.pars.get(i);
			MemRelAccess access = (MemRelAccess) Memory.accesses.get(par);
			offsets.add(access.offset);
			ImcExpr argExpr = (ImcExpr) expr.args.get(i).accept(this, arg);
			args.add(argExpr);
		}
		e = new ImcCALL(frame.label, offsets, args);

		ImcGen.exprImc.put(expr, e);
		return e;
	}

	@Override
	public ImcInstr visit(AstCastExpr expr, Stack<MemFrame> arg) {
		ImcExpr e = null;
		if (expr.type == null) return null;
		if (expr.expr == null) return null;
		// EX14
		e = (ImcExpr) expr.expr.accept(this, arg);

		// EX15 - mod 256
		SemType sType = SemAn.isType.get(expr.type);
		if (sType instanceof SemChar) {
			e = new ImcBINOP(
				ImcBINOP.Oper.MOD,
				e,
				new ImcCONST(256)
			);
		}

		ImcGen.exprImc.put(expr, e);
		return e;
	}

	@Override
	public ImcInstr visit(AstExprStmt stmt, Stack<MemFrame> arg) {
		ImcStmt s = null;
		if (stmt.expr == null) return s;
		ImcExpr e = (ImcExpr) stmt.expr.accept(this, arg);

		// ST1
		s = new ImcESTMT(e);

		ImcGen.stmtImc.put(stmt, s);
		return s;
	}

	@Override
	public ImcInstr visit(AstAssignStmt stmt, Stack<MemFrame> arg) {
		ImcStmt s = null;
		if (stmt.src == null) return s;
		if (stmt.dst == null) return s;
		ImcExpr src = (ImcExpr) stmt.src.accept(this, arg);
		ImcExpr dst = (ImcExpr) stmt.dst.accept(this, arg);

		// ST2
		s = new ImcMOVE(dst, src);

		ImcGen.stmtImc.put(stmt, s);
		return s;
	}

	@Override
	public ImcInstr visit(AstIfStmt stmt, Stack<MemFrame> arg) {
		ImcStmt s = null;
		if (stmt.cond == null) return s;
		if (stmt.thenStmt == null) return s;
		ImcExpr cond = (ImcExpr) stmt.cond.accept(this, arg);
		ImcStmt thenStmt = (ImcStmt) stmt.thenStmt.accept(this, arg);
		ImcStmt elseStmt = null;
		if (stmt.elseStmt != null)
			elseStmt = (ImcStmt) stmt.elseStmt.accept(this, arg);

		// ST2, ST3
		MemLabel posLabel = new MemLabel();
		MemLabel negLabel = new MemLabel();
		MemLabel endLabel = new MemLabel();

		Vector<ImcStmt> stmts = new Vector<ImcStmt>();
		stmts.add(new ImcCJUMP(cond, posLabel, negLabel));
		stmts.add(new ImcLABEL(negLabel));
		if (elseStmt != null) {
			stmts.add(elseStmt);
		}
		stmts.add(new ImcJUMP(endLabel));
		stmts.add(new ImcLABEL(posLabel));
		stmts.add(thenStmt);
		stmts.add(new ImcJUMP(endLabel));
		stmts.add(new ImcLABEL(endLabel));

		s = new ImcSTMTS(stmts);

		ImcGen.stmtImc.put(stmt, s);
		return s;
	}

	@Override
	public ImcInstr visit(AstWhileStmt stmt, Stack<MemFrame> arg) {
		ImcStmt s = null;
		if (stmt.cond == null) return s;
		if (stmt.bodyStmt == null) return s;
		ImcExpr cond = (ImcExpr) stmt.cond.accept(this, arg);
		s = (ImcStmt) stmt.bodyStmt.accept(this, arg);

		// ST4, ST5
		MemLabel condLabel = new MemLabel();
		MemLabel loopLabel = new MemLabel();
		MemLabel dontLoopLabel = new MemLabel();
		MemLabel endLabel = new MemLabel();
		Vector<ImcStmt> stmts = new Vector<ImcStmt>();
		stmts.add(new ImcLABEL(condLabel));
		stmts.add(new ImcCJUMP(cond, loopLabel, dontLoopLabel));
		stmts.add(new ImcLABEL(dontLoopLabel));
		stmts.add(new ImcJUMP(endLabel));
		stmts.add(new ImcLABEL(loopLabel));
		stmts.add(s);
		stmts.add(new ImcJUMP(condLabel));
		stmts.add(new ImcLABEL(endLabel));
		s = new ImcSTMTS(stmts);

		ImcGen.stmtImc.put(stmt, s);
		return s;
	}

	@Override
	public ImcInstr visit(AstDeclStmt stmt, Stack<MemFrame> arg) {
		ImcStmt s = null;
		if (stmt.decls != null)
			stmt.decls.accept(this, arg);
		if (stmt.stmt == null) return s;
		s = (ImcStmt) stmt.stmt.accept(this, arg);

		ImcGen.stmtImc.put(stmt, s);
		return s;
	}

	@Override
	public ImcInstr visit(AstStmts stmts, Stack<MemFrame> arg) {
		ImcStmt s = null;
		if (stmts.stmts == null) return s;
		Vector<ImcStmt> tmp = new Vector<ImcStmt>();
		int size = stmts.stmts.size();
		for (int i = 0; i < size; i++) {
			s = (ImcStmt) stmts.stmts.get(i).accept(this, arg);
			tmp.add(s);
		}
		s = new ImcSTMTS(tmp);

		ImcGen.stmtImc.put(stmts, s);
		return s;
	}
}
