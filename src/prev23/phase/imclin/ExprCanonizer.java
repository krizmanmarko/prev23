package prev23.phase.imclin;

import java.util.*;
import prev23.data.mem.*;
import prev23.data.imc.code.expr.*;
import prev23.data.imc.code.stmt.*;
import prev23.data.imc.visitor.*;

/**
 * Expression canonizer.
 */
public class ExprCanonizer implements ImcVisitor<ImcExpr, Vector<ImcStmt>> {

	@Override
	public ImcExpr visit(ImcBINOP binOp, Vector<ImcStmt> arg) {
		ImcTEMP l = new ImcTEMP(new MemTemp());
		arg.add(new ImcMOVE(l, binOp.fstExpr.accept(this, arg)));

		ImcTEMP r = new ImcTEMP(new MemTemp());
		arg.add(new ImcMOVE(r, binOp.sndExpr.accept(this, arg)));

		ImcBINOP b = new ImcBINOP(binOp.oper, l, r);

		ImcTEMP result = new ImcTEMP(new MemTemp());
		arg.add(new ImcMOVE(result, b));
		return result;
	}

	@Override
	public ImcExpr visit(ImcCALL call, Vector<ImcStmt> arg) {
		Vector<ImcExpr> args = new Vector<ImcExpr>();
		for (int i = 0; i < call.args.size(); i++) {
			ImcExpr e = call.args.get(i);
			ImcTEMP tmp = new ImcTEMP(new MemTemp());
			arg.add(new ImcMOVE(
				tmp,
				e.accept(this, arg)
			));
			args.add(tmp);
		}
		ImcTEMP result = new ImcTEMP(new MemTemp());
		ImcCALL fc = new ImcCALL(call.label, call.offs, args);
		arg.add(new ImcMOVE(
			result,
			fc
		));
		return result;
	}

	@Override
	public ImcExpr visit(ImcCONST constant, Vector<ImcStmt> arg) {
		return constant;
	}

	@Override
	public ImcExpr visit(ImcMEM mem, Vector<ImcStmt> arg) {
		ImcMEM result = new ImcMEM(mem.addr.accept(this, arg));
		return result;
	}

	@Override
	public ImcExpr visit(ImcNAME name, Vector<ImcStmt> arg) {
		return name;
	}

	//@Override
	//public ImcExpr visit(ImcSEXPR sExpr, Vector<ImcStmt> arg) {
	// not used in my code TODO
	//}

	@Override
	public ImcExpr visit(ImcTEMP temp, Vector<ImcStmt> arg) {
		return temp;
	}

	@Override
	public ImcExpr visit(ImcUNOP unOp, Vector<ImcStmt> arg) {
		ImcUNOP result = new ImcUNOP(
			unOp.oper,
			unOp.subExpr.accept(this, arg)
		);
		return result;
	}

}
