package prev23.phase.asmgen;

import java.util.*;
import prev23.data.mem.*;
import prev23.data.imc.code.expr.*;
import prev23.data.imc.visitor.*;
import prev23.data.asm.*;
import prev23.common.report.*;
import prev23.Compiler;


/**
 * Machine code generator for expressions.
 */
public class ExprGenerator implements ImcVisitor<MemTemp, Vector<AsmInstr>> {

	@Override
	public MemTemp visit(ImcBINOP binOp, Vector<AsmInstr> arg) {
		MemTemp reg = new MemTemp();
		MemTemp fst = binOp.fstExpr.accept(this, arg);
		MemTemp snd = binOp.sndExpr.accept(this, arg);

		String instr = null;
		Vector<MemTemp> uses = new Vector<MemTemp>();
		uses.add(fst);
		uses.add(snd);
		Vector<MemTemp> defs = new Vector<MemTemp>();
		defs.add(reg);
		Vector<MemLabel> jumps = new Vector<MemLabel>();

		if (binOp.oper == ImcBINOP.Oper.OR) {
			instr = "	OR	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
		} else if (binOp.oper == ImcBINOP.Oper.AND) {
			instr = "	AND	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
		} else if (binOp.oper == ImcBINOP.Oper.ADD) {
			instr = "	ADD	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
		} else if (binOp.oper == ImcBINOP.Oper.SUB) {
			instr = "	SUB	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
		} else if (binOp.oper == ImcBINOP.Oper.MUL) {
			instr = "	MUL	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
		} else if (binOp.oper == ImcBINOP.Oper.DIV) {
			instr = "	DIV	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
		} else if (binOp.oper == ImcBINOP.Oper.MOD) {
			instr = "	DIV	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
			instr = "	GET	`d0,rR";
			arg.add(new AsmOPER(instr, null, defs, jumps));
		} else if (binOp.oper == ImcBINOP.Oper.EQU) {
			instr = "	CMP	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
			instr = "	ZSZ	`d0,`s0,1";
			arg.add(new AsmOPER(instr, defs, defs, jumps));
		} else if (binOp.oper == ImcBINOP.Oper.NEQ) {
			instr = "	CMP	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
			instr = "	ZSNZ	`d0,`s0,1";
			arg.add(new AsmOPER(instr, defs, defs, jumps));
		} else if (binOp.oper == ImcBINOP.Oper.LTH) {
			instr = "	CMP	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
			instr = "	ZSN	`d0,`s0,1";
			arg.add(new AsmOPER(instr, defs, defs, jumps));
		} else if (binOp.oper == ImcBINOP.Oper.GTH) {
			instr = "	CMP	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
			instr = "	ZSP	`d0,`s0,1";
			arg.add(new AsmOPER(instr, defs, defs, jumps));
		} else if (binOp.oper == ImcBINOP.Oper.LEQ) {
			instr = "	CMP	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
			instr = "	ZSNP	`d0,`s0,1";
			arg.add(new AsmOPER(instr, defs, defs, jumps));
		} else if (binOp.oper == ImcBINOP.Oper.GEQ) {
			instr = "	CMP	`d0,`s0,`s1";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
			instr = "	ZSNN	`d0,`s0,1";
			arg.add(new AsmOPER(instr, defs, defs, jumps));
		} else {
			throw new Report.InternalError();
		}
		return reg;
	}

	private void storeArgument(MemTemp funArg, long offset, Vector<AsmInstr> arg) {
		// arg size is always 8 bytes (BOOL | CHAR | INT | PTR)
		// STO value, $254, offset
		String instr = null;
		Vector<MemTemp> uses = new Vector<MemTemp>();
		uses.add(funArg);
		Vector<MemTemp> defs = new Vector<MemTemp>();
		Vector<MemLabel> jumps = new Vector<MemLabel>();

		if (offset <= 0xff) {
			instr = "	STO	`s0,$254," + offset;
		} else {
			instr = "	STO	`s0,$254,`s1";
			MemTemp regz = (new ImcCONST(offset)).accept(this, arg);
			uses.add(regz);
		}
		arg.add(new AsmOPER(instr, uses, defs, jumps));
	}

	@Override
	public MemTemp visit(ImcCALL call, Vector<AsmInstr> arg) {
		// $254 - SP
		// $253 - FP
		MemTemp reg = new MemTemp();

		// set args
		for (int i = 0; i < call.args.size(); i++) {
			MemTemp funArg = call.args.get(i).accept(this, arg);
			long offset = call.offs.get(i);
			storeArgument(funArg, offset, arg);
		}

		// call
		//String instr = "	PUSHJ	`s0," + call.label.name;
		String instr = "	PUSHJ	$" + Compiler.numRegs + "," + call.label.name;
		Vector<MemTemp> uses = new Vector<MemTemp>();
		Vector<MemTemp> defs = new Vector<MemTemp>();
		Vector<MemLabel> jumps = new Vector<MemLabel>();
		jumps.add(call.label);
		long taintedRegs = Compiler.numRegs;
		//MemTemp regx = (new ImcCONST(taintedRegs)).accept(this, arg);
		//uses.add(regx);
		arg.add(new AsmOPER(instr, uses, defs, jumps));

		// retval
		instr = "	LDO	`d0,$254,0";
		uses = new Vector<MemTemp>();
		defs = new Vector<MemTemp>();
		defs.add(reg);
		jumps = new Vector<MemLabel>();
		arg.add(new AsmOPER(instr, uses, defs, jumps));

		return reg;
	}

	@Override
	public MemTemp visit(ImcCONST constant, Vector<AsmInstr> arg) {
		MemTemp reg = new MemTemp();
		String instr = null;
		Vector<MemTemp> uses = new Vector<MemTemp>();
		Vector<MemTemp> defs = new Vector<MemTemp>();
		defs.add(reg);
		Vector<MemLabel> jumps = new Vector<MemLabel>();

		long h = (constant.value >> 48) & 0xffff; // maybe 0x7fff
		long mh = (constant.value >> 32) & 0xffff;
		long ml = (constant.value >> 16) & 0xffff;
		long l = (constant.value >> 0) & 0xffff;

		instr = "	XOR	`d0,$0,$0";
		arg.add(new AsmOPER(instr, uses, defs, jumps));

		uses = new Vector<MemTemp>();
		uses.add(reg);
		if (h != 0) {
			instr = "	INCH	`d0," + h;
			arg.add(new AsmOPER(instr, uses, defs, jumps));
		}
		if (mh != 0) {
			instr = "	INCMH	`d0," + mh;
			arg.add(new AsmOPER(instr, uses, defs, jumps));
		}
		if (ml != 0) {
			instr = "	INCML	`d0," + ml;
			arg.add(new AsmOPER(instr, uses, defs, jumps));
		}
		if (l != 0) {
			instr = "	INCL	`d0," + l;
			arg.add(new AsmOPER(instr, uses, defs, jumps));
		}
		return reg;
	}

	@Override
	public MemTemp visit(ImcMEM mem, Vector<AsmInstr> arg) {
		MemTemp reg = new MemTemp();

		String instr = "	LDO	`d0,`s0,0";
		Vector<MemTemp> uses = new Vector<MemTemp>();
		uses.add(mem.addr.accept(this, arg));
		Vector<MemTemp> defs = new Vector<MemTemp>();
		defs.add(reg);
		Vector<MemLabel> jumps = new Vector<MemLabel>();

		arg.add(new AsmOPER(instr, uses, defs, jumps));
		return reg;
	}

	@Override
	public MemTemp visit(ImcNAME name, Vector<AsmInstr> arg) {
		MemTemp reg = new MemTemp();
		String instr = "	LDA	`d0," + name.label.name;
		Vector<MemTemp> uses = new Vector<MemTemp>();
		Vector<MemTemp> defs = new Vector<MemTemp>();
		defs.add(reg);
		Vector<MemLabel> jumps = new Vector<MemLabel>();

		arg.add(new AsmOPER(instr, uses, defs, jumps));
		return reg;
	}

	@Override
	public MemTemp visit(ImcSEXPR sExpr, Vector<AsmInstr> arg) {
		// imclin removed this
		throw new Report.InternalError();
	}

	@Override
	public MemTemp visit(ImcTEMP temp, Vector<AsmInstr> arg) {
		return temp.temp;
	}

	@Override
	public MemTemp visit(ImcUNOP unOp, Vector<AsmInstr> arg) {
		MemTemp reg = new MemTemp();
		MemTemp sub = unOp.subExpr.accept(this, arg);

		String instr = null;
		Vector<MemTemp> uses = new Vector<MemTemp>();
		uses.add(sub);
		Vector<MemTemp> defs = new Vector<MemTemp>();
		defs.add(reg);
		Vector<MemLabel> jumps = new Vector<MemLabel>();

		if (unOp.oper == ImcUNOP.Oper.NOT) {
			instr = "	NEG	`d0,`s0";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
			instr = "	ADD	`d0,`s0,1";
			arg.add(new AsmOPER(instr, defs, defs, jumps));
		} else if (unOp.oper == ImcUNOP.Oper.NEG) {
			instr = "	NEG	`d0,`s0";
			arg.add(new AsmOPER(instr, uses, defs, jumps));
		}
		return reg;
	}
}
