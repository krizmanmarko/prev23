package prev23.phase.regall;

import java.util.*;
import prev23.data.mem.*;
import prev23.common.report.*;

public class Node {

	private MemTemp id;
	private HashSet<Node> connections = new HashSet<Node>();
	private boolean potentialSpill = false;

	// 0-(numRegs - 1) colors, -1 init, numRegs spill
	private int color = -1;

	public Node(MemTemp temp) {
		this.id = temp;
	}

	public Node(MemTemp temp, Set<Node> knownConnections) {
		this.id = temp;
		connections.addAll(knownConnections);
	}

	public void setColor(int color, int numRegs) {
		if (color < 0)
			throw new Report.Error("color cannot be negative");
		else if (numRegs <= color) {
			if (potentialSpill)
				this.color = numRegs;
			else
				throw new Report.Error(this + "(Tried to spill without potential)");
		} else {
			this.color = color;
		}
	}

	public int getColor() {
		return this.color;
	}

	public void addConnection(Node n) {
		if (this == n) return;
		if (this.connections.add(n)) {
			n.addConnection(this);
		}
	}

	public void addConnections(Set<Node> knownConnections) {
		for (Node n : knownConnections) {
			this.addConnection(n);
		}
	}

	public void delConnection(Node n) {
		if (this == n) return;
		if (this.connections.remove(n)) {
			n.delConnection(this);
		}
	}

	public void delConnections(Set<Node> connectionsToRemove) {
		for (Node n : connectionsToRemove) {
			this.delConnection(n);
		}
	}

	public HashSet<Node> connections() {
		return new HashSet<Node>(this.connections);
	}

	public void markPotentialSpill() {
		this.potentialSpill = true;
	}

	public MemTemp id() {
		return this.id;
	}

	public int degree() {
		return this.connections.size();
	}

	public void print() {
		System.out.println(this);
		for (Node n : this.connections) {
			System.out.println("	-> " + n);
		}
	}

	public void printSpill() {
		if (this.potentialSpill)
			System.out.println(this + " (potential spill)");
		else
			System.out.println(this);
	}

	@Override
	public String toString() {
		return "Node: " + this.id + "[" + this.color + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Node) {
			Node n = (Node) o;
			if (n.id == this.id) {
				return true;
			}
		}
		return false;
	}
}
