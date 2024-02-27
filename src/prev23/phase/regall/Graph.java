package prev23.phase.regall;

import prev23.phase.regall.Node;
import prev23.data.mem.*;
import java.util.*;

public class Graph {

	private HashSet<Node> nodes = new HashSet<Node>();

	public void addNode(Node n) {
		for (Node n2 : this.nodes)
			if (n2.equals(n)) return;
		this.nodes.add(n);
	}

	public void addNodes(Set<Node> _nodes) {
		for (Node n : _nodes)
			this.addNode(n);
	}

	public Node findNode(MemTemp temp) {
		for (Node n : this.nodes)
			if (n.id() == temp) return n;
		return null;
	}

	public void removeNode(Node n) {
		if (n == null) return;
		this.nodes.remove(n);
		for (Node i : n.connections()) {
			i.delConnection(n);
		}
	}

	public Node getLowDegreeNode(int maxDegree) {
		for (Node n : this.nodes)
			if (n.degree() < maxDegree)
				return n;
		return null;
	}

	public Node getHighDegreeNode(int minDegree) {
		for (Node n : this.nodes)
			if (n.degree() >= minDegree)
				return n;
		return null;
	}

	public int size() {
		return this.nodes.size();
	}

	public HashSet<Node> nodes() {
		return new HashSet<Node>(nodes);
	}

	public void print() {
		System.out.println("Graph nodes: " + this.nodes.size());
		for (Node n : this.nodes) {
			System.out.println("	" + n);
		}
	}

	public void printMore() {
		System.out.println("Graph nodes: " + this.nodes.size());
		for (Node n : this.nodes) {
			System.out.print("    ");
			n.print();
		}
	}
}
