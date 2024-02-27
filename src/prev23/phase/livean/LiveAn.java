package prev23.phase.livean;

import prev23.data.mem.*;
import prev23.data.asm.*;
import prev23.phase.*;
import prev23.phase.asmgen.*;
import prev23.common.report.*;
import prev23.Compiler;


import java.util.*;

/**
 * Liveness analysis.
 */
public class LiveAn extends Phase {

	public LiveAn() {
		super("livean");
	}

	private HashSet<MemTemp> getNextIn(AsmInstr instr) {
		HashSet<MemTemp> in = new HashSet<MemTemp>();
		HashSet<MemTemp> tmp = new HashSet<MemTemp>();
		tmp.addAll(instr.out());
		tmp.removeAll(instr.defs());
		in.addAll(instr.uses());
		in.addAll(tmp);
		return in;
	}

	private HashSet<AsmInstr> getSuccessor(
			Code code,
			HashMap<String, AsmLABEL> labels,
			int index) {

		AsmInstr instr = code.instrs.get(index);
		HashSet<AsmInstr> succ = new HashSet<AsmInstr>();

		if (instr.jumps().size() == 0) {
			if (index + 1 >= code.instrs.size()) return null;
			succ.add(code.instrs.get(index + 1));
		} else {
			for (MemLabel l : instr.jumps()) {
				AsmLABEL label = labels.get(l.name);
				if (label != null) {
					succ.add(label);
				} else {
					//Report.info("end of function label");
				}
			}
			if (instr.toString().contains("PUSHJ")) {
				succ.add(code.instrs.get(index + 1));
			}
		}
		return succ;
	}

	private HashSet<MemTemp> getNextOut(AsmInstr instr, Set<AsmInstr> succ) {
		HashSet<MemTemp> out = new HashSet<MemTemp>();
		for (AsmInstr s : succ) {
			out.addAll(s.in());
		}
		return out;
	}

	private boolean checkIfChanged(int[] pin, int[] pout, int[] in, int[] out) {
		for (int i = 0; i < pin.length; i++) {
			if (pin[i] != in[i]) return true;
		}
		for (int i = 0; i < pout.length; i++) {
			if (pout[i] != out[i]) return true;
		}
		return false;
	}

	public void analyse(Code code) {
		int[] prevInSizes = new int[code.instrs.size()];
		int[] prevOutSizes = new int[code.instrs.size()];
		int[] inSizes = new int[code.instrs.size()];
		int[] outSizes = new int[code.instrs.size()];


		HashMap<String, AsmLABEL> labels = new HashMap<String, AsmLABEL>();
		for (AsmInstr instr : code.instrs) {
			if (instr instanceof AsmOPER) {
				((AsmOPER) instr).removeAllFromIn();
				((AsmOPER) instr).removeAllFromOut();
			}
			if (instr instanceof AsmLABEL) {
				labels.put(instr.toString(), (AsmLABEL) instr);
			}
		}

		boolean changed = true;
		while (changed) {
			for (int i = 0; i < code.instrs.size(); i++) {
				AsmInstr instr = code.instrs.get(i);
				HashSet<MemTemp> prevIn = instr.in();
				HashSet<MemTemp> prevOut = instr.out();

				instr.addInTemps(getNextIn(instr));
				prevInSizes[i] = inSizes[i];
				inSizes[i] = instr.in().size();

				HashSet<AsmInstr> successor = getSuccessor(code, labels, i);
				instr.addOutTemp(getNextOut(instr, successor));
				prevOutSizes[i] = outSizes[i];
				outSizes[i] = instr.out().size();
			}
			changed = checkIfChanged(prevInSizes, prevOutSizes,
						 inSizes, outSizes);
		}
		//for (int i = 0; i < code.instrs.size(); i++) {
		//	System.out.println("instr: " + code.instrs.get(i));
		//	System.out.println("next : " + getSuccessor(code, labels, i));
		//}
	}

	public void analysis() {
		for (Code code : AsmGen.codes) {
			analyse(code);
		}
	}

	public void log() {
		if (logger == null)
			return;
		for (Code code : AsmGen.codes) {
			logger.begElement("code");
			logger.addAttribute("entrylabel", code.entryLabel.name);
			logger.addAttribute("exitlabel", code.exitLabel.name);
			logger.addAttribute("tempsize", Long.toString(code.tempSize));
			code.frame.log(logger);
			logger.begElement("instructions");
			for (AsmInstr instr : code.instrs) {
				logger.begElement("instruction");
				logger.addAttribute("code", instr.toString());
				logger.begElement("temps");
				logger.addAttribute("name", "use");
				for (MemTemp temp : instr.uses()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.begElement("temps");
				logger.addAttribute("name", "def");
				for (MemTemp temp : instr.defs()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.begElement("temps");
				logger.addAttribute("name", "in");
				for (MemTemp temp : instr.in()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.begElement("temps");
				logger.addAttribute("name", "out");
				for (MemTemp temp : instr.out()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.endElement();
			}
			logger.endElement();
			logger.endElement();
		}
	}
}
