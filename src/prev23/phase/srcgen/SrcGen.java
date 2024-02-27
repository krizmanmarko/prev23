package prev23.phase.srcgen;

import java.util.*;
import java.io.PrintWriter;
import java.io.IOException;

import prev23.phase.*;
import prev23.data.asm.*;
import prev23.data.lin.*;
import prev23.common.report.*;
import prev23.phase.asmgen.*;
import prev23.phase.regall.*;
import prev23.phase.imclin.*;
import prev23.Compiler;


/**
 * Generate source code
 */
public class SrcGen extends Phase {

	private final long numRegs = Compiler.numRegs;
	public SrcGen() {
		super("srcgen");
	}

	private Vector<String> createData() {
		Vector<String> data = new Vector<String>();
		data.add("	LOC	Data_Segment");

		// for getchar
		data.add("buf	OCTA	0,0"); // 8 chars (TRAP char is 1 byte)
		data.add("args	OCTA	buf,2"); // including \0

		for (LinDataChunk chunk : ImcLin.dataChunks()) {
			if (chunk.init != null) {
				// strings
				char c = chunk.init.charAt(0);
				int v = c;
				data.add(chunk.label.name + "	OCTA	" + v);
				for (int i = 1; i < chunk.init.length(); i++) {
					c = chunk.init.charAt(i);
					v = c;
					data.add("	OCTA	" + v);
				}
				data.add("	OCTA	0");
			} else {
				// primitive type and complex type
				long nrElem = chunk.size / 8;
				data.add(chunk.label.name + "	OCTA	165");
				for (int i = 1; i < nrElem; i++) {
					data.add("	OCTA	165");
				}
			}
		}
		return data;
	}

	private Vector<String> initCode() {
		Vector<String> instrs = new Vector<String>();
		instrs.add("	LOC	#100"); // non privileged text section
		instrs.add("Main	PUT	rG,252");
		instrs.add("SP	GREG	0");
		instrs.add("FP	GREG	0");
		instrs.add("HP	GREG	0");

		instrs.addAll(loadConst(0x7ffffffff000L, "$254"));
		instrs.addAll(loadConst(0x602000L, "$252"));
		instrs.addAll(loadConst(Compiler.numRegs, "$0"));
		instrs.add("	PUSHJ	$" + Compiler.numRegs + ",_main");
		instrs.add("End	TRAP	0,Halt,0");
		return instrs;
	}

	private Vector<String> stdlib() {
		Vector<String> instrs = new Vector<String>();

		instrs.add("_getChar	SWYM");
		instrs.add("	LDA	$255,args");
		instrs.add("	TRAP	0,Fgets,StdIn");
		instrs.add("	LDA	$0,buf");
		instrs.add("	LDB	$0,$0,0");
		instrs.add("	STO	$0,$254,0");
		instrs.add("	POP	0,0");

		instrs.add("_putChar	SWYM");
		instrs.add("	LDO	$0,$254,8");	// we have char in $0
		instrs.add("	LDA	$255,buf");
		instrs.add("	STB	$0,$255,0");	// char is in buf
		instrs.add("	TRAP	0,Fputs,StdOut");
		instrs.add("	POP	0,0");

		instrs.add("_new	SWYM");
		instrs.add("	LDO	$0,$254,8");	// size
		instrs.add("	STO	$252,$254,0");
		instrs.add("	ADD	$252,$252,$0");
		instrs.add("	POP	0,0");

		instrs.add("_del	SWYM");
		instrs.add("	POP	0,0");

		instrs.add("_exit	SWYM");
		instrs.add("	JMP	End");

		return instrs;
	}

	private Vector<String> loadConst(long num, String reg) {
		Vector<String> v = new Vector<String>();

		long h = (num >> 48) & 0xffff; // maybe 0x7fff
		long mh = (num >> 32) & 0xffff;
		long ml = (num >> 16) & 0xffff;
		long l = (num >> 0) & 0xffff;

		v.add("	XOR	" + reg + ",$0,$0");
		if (h != 0) v.add("	INCH	" + reg + "," + h);
		if (mh != 0) v.add("	INCMH	" + reg + "," + mh);
		if (ml != 0) v.add("	INCML	" + reg + "," + ml);
		if (l != 0) v.add("	INCL	" + reg + "," + l);

		return v;

	}

	private Vector<String> createPrologue(Code code) {
		// stack - zadnja zasedena
		// tempSize -> ze pushj to nardi
		long localVars = code.frame.locsSize;
		long savedFP = 8;
		long retaddr = 8;
		//long storedRegs = this.numRegs * 8;	-> ze pushj nardi
		long tempVars = code.tempSize;
		long argsSize = code.frame.argsSize;
		long stackSize =
			localVars +
			savedFP +
			retaddr +
		//	storedRegs +			-> ze pushj nardi
			tempVars +
			argsSize;

		Vector<String> prologue = new Vector<String>();
		prologue.add(code.frame.label.name + "	SWYM");
		prologue.addAll(loadConst((long) -localVars, "$0"));
		// push bp
		prologue.add("	SUB	$0,$0,8");
		prologue.add("	STO	$253,$254,$0");
		// push retaddr
		prologue.add("	SUB	$0,$0,8");
		prologue.add("	GET	$1,rJ");
		prologue.add("	STO	$1,$254,$0");
		// mov fp, sp
		prologue.add("	SET	$253,$254");
		// sub sp, sp, size
		prologue.addAll(loadConst((long) stackSize, "$0"));
		prologue.add("	SUB	$254,$254,$0");
		prologue.add("	JMP	" + code.entryLabel.name);

		return prologue;
	}

	private Vector<String> createEpilogue(Code code) {
		Vector<String> epilogue = new Vector<String>();
		epilogue.add(code.exitLabel.name + "	SWYM");
		// save retval

		Integer retval = RegAll.tempToReg.get(code.frame.RV);
		if (retval != null)
			epilogue.add("	STO	$" + retval + ",$253");

		// mov sp, fp
		epilogue.add("	SET	$254,$253");
		// pop bp
		epilogue.addAll(loadConst((long) -code.frame.locsSize, "$0"));
		epilogue.add("	SUB	$0,$0,8");
		epilogue.add("	LDO	$253,$254,$0");
		// return
		epilogue.add("	SUB	$0,$0,8");
		epilogue.add("	LDO	$0,$254,$0");
		epilogue.add("	PUT	rJ,$0");
		epilogue.add("	POP	0,0");
		return epilogue;
	}

	private void writeToFile(Vector<String> instrs, String path) {
		try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
			for (String instr : instrs) {
				writer.println(instr);
			}
		} catch (Exception e) {
			Report.warning("Writing to file FAILED");
		}
	}

	public void finish() {
		Vector<String> source = new Vector<String>();
		source.addAll(createData());
		source.addAll(initCode());
		source.addAll(stdlib());
		for (Code code : AsmGen.codes) {
			if (code.frame.label.name.equals("_getChar")) continue;
			if (code.frame.label.name.equals("_putChar")) continue;
			if (code.frame.label.name.equals("_new")) continue;
			if (code.frame.label.name.equals("_del")) continue;
			if (code.frame.label.name.equals("_exit")) continue;
			source.addAll(createPrologue(code));
			for (AsmInstr instr : code.instrs) {
				if (instr instanceof AsmLABEL) {
					source.add(instr + "	SWYM");
				} else {
					source.add(instr.toString(RegAll.tempToReg));
				}
			}
			source.addAll(createEpilogue(code));
		}
		writeToFile(source, "/tmp/test.mms");
	}

}
