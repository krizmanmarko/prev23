package prev23.phase.imclin;

import java.util.*;

import prev23.common.report.*;
import prev23.data.mem.*;
import prev23.data.imc.code.expr.*;
import prev23.data.imc.code.stmt.*;
import prev23.data.imc.visitor.*;

/**
 * Statement canonizer.
 */
public class StmtCanonizer implements ImcVisitor<Vector<ImcStmt>, Object> {

	private ExprCanonizer ec = new ExprCanonizer();

	public Vector<ImcStmt> visit(ImcCJUMP cjump, Object arg) {
		Vector<ImcStmt> v = new Vector<ImcStmt>();
		v.add(new ImcCJUMP(
			cjump.cond.accept(ec, v),
			cjump.posLabel,
			cjump.negLabel
		));
		return v;
	}

	public Vector<ImcStmt> visit(ImcESTMT eStmt, Object arg) {
		Vector<ImcStmt> v = new Vector<ImcStmt>();
		if (eStmt.expr == null) return v;
		v.add(new ImcESTMT(eStmt.expr.accept(ec, v)));
		return v;
	}

	public Vector<ImcStmt> visit(ImcJUMP jump, Object arg) {
		Vector<ImcStmt> v = new Vector<ImcStmt>();
		v.add(jump);
		return v;
	}

	public Vector<ImcStmt> visit(ImcLABEL label, Object arg) {
		Vector<ImcStmt> v = new Vector<ImcStmt>();
		v.add(label);
		return v;
	}

	public Vector<ImcStmt> visit(ImcMOVE move, Object arg) {
		Vector<ImcStmt> v = new Vector<ImcStmt>();

		ImcTEMP tdst = new ImcTEMP(new MemTemp());
		ImcTEMP tsrc = new ImcTEMP(new MemTemp());

		ImcExpr dst = move.dst.accept(ec, v);
		ImcExpr src = move.src.accept(ec, v);
		if (dst instanceof ImcMEM) {
			ImcMEM mem = (ImcMEM) dst;
			v.add(new ImcMOVE(tdst, mem.addr));
			v.add(new ImcMOVE(tsrc, src));
			v.add(new ImcMOVE(new ImcMEM(tdst), tsrc));
		} else if (dst instanceof ImcTEMP) {
			tdst = (ImcTEMP) move.dst.accept(ec, v);
			v.add(new ImcMOVE(tsrc, src));
			v.add(new ImcMOVE(tdst, tsrc));
		}

		return v;
	}

	public Vector<ImcStmt> visit(ImcSTMTS stmts, Object arg) {
		Vector<ImcStmt> v = new Vector<ImcStmt>();
		if (stmts.stmts == null) return v;
		for (int i = 0; i < stmts.stmts.size(); i++) {
			v.addAll(stmts.stmts.get(i).accept(this, arg));
		}
		return v;
	}

}
