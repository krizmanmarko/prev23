package prev23.phase.memory;

import prev23.common.report.*;
import prev23.data.ast.tree.*;
import prev23.data.ast.tree.decl.*;
import prev23.data.ast.tree.expr.*;
import prev23.data.ast.tree.type.*;
import prev23.data.ast.visitor.*;
import prev23.data.typ.*;
import prev23.data.mem.*;
import prev23.phase.seman.*;

import java.util.LinkedList;

/**
 * Computing memory layout: frames and accesses.
 */
public class MemEvaluator extends AstFullVisitor<Object, MemEvaluator.Context> {

	private LinkedList<String> nameUsed = new LinkedList<String>();

	private MemLabel getLabel(String name) {
		if (nameUsed.contains(name))
			return new MemLabel();
		nameUsed.addFirst(name);
		return new MemLabel(name);
	}

	protected abstract class Context {
	}

	private class FunContext extends Context {
		public int depth;
		public long parsSize;
		public long localSize;
		public long argsSize;
	}

	private class RecContext extends Context {
		public long recordSize;
	}

	@Override
	public Object visit(AstFunDecl decl, Context arg) {
		FunContext ctx = new FunContext();
		MemLabel label = null;

		// depth
		if (arg == null) {
			label = getLabel(decl.name);
			ctx.depth = 0;
		} else {
			label = new MemLabel();
			if (arg instanceof FunContext)
				ctx.depth = ((FunContext) arg).depth + 1;
			else
				Report.warning("function declaration context is not FunContext. Skipping");
		}

		if (decl.pars != null)
			decl.pars.accept(this, ctx);
		if (decl.type != null)
			decl.type.accept(this, ctx);
		if (decl.stmt != null)
			decl.stmt.accept(this, ctx);

		MemFrame frame = new MemFrame(
			label,
			ctx.depth,
			ctx.localSize,	// vars + SL
			ctx.argsSize + 8
		);

		Memory.frames.put(decl, frame);
		return null;
	}

	@Override
	public Object visit(AstVarDecl decl, Context arg) {
		if (decl.type != null)
			decl.type.accept(this, arg);

		MemAccess access = null;
		long size = SemAn.isType.get(decl.type).size();
		if (arg != null) {
			FunContext ctx = (FunContext) arg;
			// frame
			ctx.localSize += SemAn.isType.get(decl.type).size();
			// access
			long offset = -ctx.localSize;
			access = new MemRelAccess(size, offset, ctx.depth);
		} else {
			MemLabel label = getLabel(decl.name);
			access = new MemAbsAccess(size, label);
		}

		Memory.accesses.put(decl, access);
		return null;
	}

	@Override
	public Object visit(AstCallExpr expr, Context arg) {
		if (expr.args != null)
			expr.args.accept(this, arg);

		AstFunDecl fDecl = (AstFunDecl) SemAn.declaredAt.get(expr);
		FunContext ctx = (FunContext) arg;
		long size = 0;
		for (int i = 0; i < fDecl.pars.size(); i++) {
			AstParDecl pDecl = (AstParDecl) fDecl.pars.get(i);
			size += SemAn.isType.get(pDecl.type).size();
		}

		if (ctx.argsSize < size)
			ctx.argsSize = size;
		return null;
	}

	@Override
	public Object visit(AstParDecl decl, Context arg) {
		if (decl.type == null)
			decl.type.accept(this, arg);
		FunContext ctx = (FunContext) arg;
		long size = SemAn.isType.get(decl.type).size();
		// frame
		ctx.parsSize += size;

		// access
		long offset = ((FunContext) arg).parsSize;
		MemAccess access = new MemRelAccess(size, offset, ctx.depth);

		Memory.accesses.put(decl, access);
		return null;
	}

	@Override
	public Object visit(AstRecType type, Context arg) {
		if (type.comps != null)
			type.comps.accept(this, new RecContext());
		return null;
	}

	@Override
	public Object visit(AstCmpDecl decl, Context arg) {
		if (decl.type != null)
			decl.type.accept(this, arg);
		RecContext ctx = (RecContext) arg;
		long size = SemAn.isType.get(decl.type).size();
		long offset = ctx.recordSize;
		MemAccess access = new MemRelAccess(size, offset, 0);
		ctx.recordSize += size;
		Memory.accesses.put(decl, access);
		return null;
	}

	@Override
	public Object visit(AstAtomExpr expr, Context arg) {
		if (expr.type == AstAtomExpr.Type.STR) {
			int ssize = expr.value.length();
			String e = expr.value.substring(1, ssize - 1);
			e = e.replaceAll("\\\\\"", "\"");	// \" -> "
			e = e.replaceAll("\\\\\\\\", "\\\\");	// \\ -> \
			long size = (e.length() + 1) * (new SemChar()).size();
			MemLabel label = new MemLabel();
			MemAbsAccess access = new MemAbsAccess(size, label, e);
			Memory.strings.put(expr, access);
		}
		return null;
	}
}
