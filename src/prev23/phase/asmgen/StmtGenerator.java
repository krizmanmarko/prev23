package prev23.phase.asmgen;

import java.util.*;
import prev23.data.imc.code.expr.*;
import prev23.data.imc.code.stmt.*;
import prev23.data.imc.visitor.*;
import prev23.data.mem.*;
import prev23.data.asm.*;
import prev23.common.report.*;

/**
 * Machine code generator for statements.
 */
public class StmtGenerator implements ImcVisitor<Vector<AsmInstr>, Object> {

	private ExprGenerator eg = new ExprGenerator();

	@Override
	public Vector<AsmInstr> visit(ImcCJUMP cjump, Object arg) {
		Vector<AsmInstr> v = new Vector<AsmInstr>();

		String instr = "	BNZ	`s0," + cjump.posLabel.name;
		Vector<MemTemp> uses = new Vector<MemTemp>();
		uses.add(cjump.cond.accept(eg, v));
		Vector<MemTemp> defs = new Vector<MemTemp>();
		Vector<MemLabel> jumps = new Vector<MemLabel>();
		jumps.add(cjump.posLabel);
		jumps.add(cjump.negLabel);

		v.add(new AsmOPER(instr, uses, defs, jumps));
		return v;
	}

	@Override
	public Vector<AsmInstr> visit(ImcESTMT eStmt, Object arg) {
		Vector<AsmInstr> v = new Vector<AsmInstr>();

		eStmt.expr.accept(eg, v);

		return v;
	}

	@Override
	public Vector<AsmInstr> visit(ImcJUMP jump, Object arg) {
		Vector<AsmInstr> v = new Vector<AsmInstr>();

		String instr = "	JMP	" + jump.label.name;
		Vector<MemTemp> uses = new Vector<MemTemp>();
		Vector<MemTemp> defs = new Vector<MemTemp>();
		Vector<MemLabel> jumps = new Vector<MemLabel>();
		jumps.add(jump.label);

		v.add(new AsmOPER(instr, uses, defs, jumps));
		return v;
	}

	@Override
	public Vector<AsmInstr> visit(ImcLABEL label, Object arg) {
		Vector<AsmInstr> v = new Vector<AsmInstr>();
		v.add(new AsmLABEL(label.label));
		return v;
	}

	@Override
	public Vector<AsmInstr> visit(ImcMOVE move, Object arg) {
		Vector<AsmInstr> v = new Vector<AsmInstr>();

		String instr = null;
		Vector<MemTemp> defs = new Vector<MemTemp>();
		Vector<MemTemp> uses = new Vector<MemTemp>();
		uses.add(move.src.accept(eg, v));
		if (move.dst instanceof ImcMEM) { // write
			instr = "	STO	`s0,`s1,0";
			uses.add(((ImcMEM) move.dst).addr.accept(eg, v));
			Vector<MemLabel> jumps = new Vector<MemLabel>();
			v.add(new AsmOPER(instr, uses, defs, jumps));
		} else { // read
			instr = "	SET	`d0,`s0";
			defs.add(move.dst.accept(eg, v));
			v.add(new AsmMOVE(instr, uses, defs));
		}

		return v;
	}

	@Override
	public Vector<AsmInstr> visit(ImcSTMTS stmts, Object arg) {
		// imclin removed this
		throw new Report.InternalError();
	}

}
