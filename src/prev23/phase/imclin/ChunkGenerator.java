package prev23.phase.imclin;

import java.util.*;

import prev23.data.ast.tree.decl.*;
import prev23.data.ast.tree.expr.*;
import prev23.data.ast.visitor.*;
import prev23.data.mem.*;
import prev23.data.imc.code.expr.*;
import prev23.data.imc.code.stmt.*;
import prev23.data.lin.*;
import prev23.phase.imcgen.*;
import prev23.phase.memory.*;

public class ChunkGenerator extends AstFullVisitor<Object, Object> {

	private Vector<ImcStmt> reorderChunks(Vector<ImcStmt> code) {
		Vector<ImcStmt> v = new Vector<ImcStmt>();
		return code; // TODO return v;
	}

	private Vector<ImcStmt> transform(ImcStmt stmt) {
		Vector<ImcStmt> v = null;
		StmtCanonizer sc = new StmtCanonizer();

		// canonize
		v = stmt.accept(sc, null);

		//v = reorderChunks(v);

		return v;
	}

	@Override
	public Object visit(AstFunDecl decl, Object arg) {
		if (decl.pars != null) decl.pars.accept(this, arg);
		if (decl.type != null) decl.type.accept(this, arg);
		ImcStmt stmt = null;
		if (decl.stmt != null) {
			stmt = ImcGen.stmtImc.get(decl.stmt);
			decl.stmt.accept(this, arg);
		}
		MemFrame frame = Memory.frames.get(decl);
		MemLabel entry = new MemLabel();
		MemLabel exit = new MemLabel();

		Vector<ImcStmt> stmts = new Vector<ImcStmt>();
		stmts.add(new ImcLABEL(entry));
		if (decl.stmt != null)
			stmts.addAll(transform(stmt));
		stmts.add(new ImcJUMP(exit));

		LinCodeChunk chunk = new LinCodeChunk(
			frame,
			stmts,
			entry,
			exit
		);

		ImcLin.addCodeChunk(chunk);
		return null;
	}

	@Override
	public Object visit(AstVarDecl decl, Object arg) {
		MemAccess access = Memory.accesses.get(decl);
		if (access instanceof MemAbsAccess) {
			MemAbsAccess a = (MemAbsAccess) access;
			ImcLin.addDataChunk(new LinDataChunk(a));
		}
		return null;
	}

	@Override
	public Object visit(AstAtomExpr expr, Object arg) {
		if (expr.type != null) {
			if (expr.type == AstAtomExpr.Type.STR) {
				MemAbsAccess a = Memory.strings.get(expr);
				ImcLin.addDataChunk(new LinDataChunk(a));
			}
		}
		return null;
	}

}
