package prev23.phase.seman;

import prev23.common.report.*;
import prev23.data.ast.tree.*;
import prev23.data.ast.tree.decl.*;
import prev23.data.ast.tree.expr.*;
import prev23.data.ast.tree.stmt.*;
import prev23.data.ast.tree.type.*;
import prev23.data.ast.visitor.*;
import prev23.data.typ.*;

/**
 * AddrResolver resolver.
 */
public class AddrResolver extends AstFullVisitor<Boolean, Integer> {
	@Override
	public Boolean visit(AstTrees<? extends AstTree> trees, Integer arg) {
		for (AstTree t: trees)
			if (t != null)
				t.accept(this, 1);
		return false;
	}

	// removes lvalue tag from right side of assign statement
	@Override
	public Boolean visit(AstAssignStmt stmt, Integer arg) {
		Boolean isAddr = false;
		if (stmt.dst != null)
			isAddr = stmt.dst.accept(this, arg);
		if (stmt.src != null)
			stmt.src.accept(this, 0);
		return isAddr;
	}

	@Override
	public Boolean visit(AstNameExpr expr, Integer arg) {
		Boolean isAddr = false;
		if (arg == 0) return isAddr;

		AstNameDecl nDecl = SemAn.declaredAt.get(expr);
		if (nDecl instanceof AstVarDecl)
			isAddr = true;
		if (nDecl instanceof AstParDecl)
			isAddr = true;

		SemAn.isAddr.put(expr, isAddr);
		return isAddr;
	}

	@Override
	public Boolean visit(AstSfxExpr expr, Integer arg) {
		Boolean isAddr = false;
		if (arg == 0) return isAddr;
		if (expr.expr == null) return isAddr;

		if (SemAn.ofType.get(expr.expr) instanceof SemPtr)
			isAddr = true;

		SemAn.isAddr.put(expr, isAddr);
		return isAddr;
	}

	@Override
	public Boolean visit(AstArrExpr expr, Integer arg) {
		Boolean isAddr = false;
		if (arg == 0) return isAddr;
		if (expr.arr == null) return isAddr;

		isAddr = expr.arr.accept(this, arg);

		SemAn.isAddr.put(expr, isAddr);
		return isAddr;
	}

	@Override
	public Boolean visit(AstRecExpr expr, Integer arg) {
		Boolean isAddr = false;
		if (arg == 0) return isAddr;
		if (expr.rec == null) return isAddr;

		isAddr = expr.rec.accept(this, arg);

		SemAn.isAddr.put(expr, isAddr);
		return isAddr;
	}
}
