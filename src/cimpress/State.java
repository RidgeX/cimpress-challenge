package cimpress;

import cimpress.json.*;
import java.util.*;

public class State implements Comparable<State> {
	public BitSet grid;
	public Set<Mask> used;
	public int p;
	public double w;
	
	public State(BitSet grid, Set<Mask> used, int p, double w) {
		this.grid = grid;
		this.used = used;
		this.p = p;
		this.w = w;
	}
	
	public int compareTo(State s) {
		double d1 = (s.p - p) * w;
		double d2 = (s.grid.cardinality() - grid.cardinality()) * (1-w);
		int d = (int)(d1 + d2);
		if (d != 0) return d;
		return used.size() - s.used.size();
	}
	
	public String toString() {
		return used.toString();
	}
}
