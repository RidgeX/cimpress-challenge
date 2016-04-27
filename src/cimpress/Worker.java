package cimpress;

import cimpress.json.*;
import java.util.*;

public class Worker implements Runnable {
	private Solver solver;
	private double weight;
	private long timeLimit;
	private int maxSize;
	private int numCells;
	private BitSet grid;
	private Map<Integer, List<Mask>> masks;
	
	public Worker(Solver solver, double weight, long timeLimit) {
		this.solver = solver;
		this.weight = weight;
		this.timeLimit = timeLimit;
		maxSize = solver.maxSize;
		numCells = solver.numCells;
		grid = solver.grid;
		masks = solver.masks;
	}
	
	public void run() {
		PriorityQueue<State> queue = new PriorityQueue<State>();
		queue.add(new State(grid, new HashSet<Mask>(), 0, weight));
		long start = System.currentTimeMillis();
		while (!queue.isEmpty()) {
			if ((System.currentTimeMillis() - start) >= timeLimit) return;
			State s = queue.remove();
			int numCellsFilled = s.grid.cardinality();
			if (numCellsFilled == numCells) {
				solver.updateBest(s);
				continue;
			}
			for (int n = maxSize; n >= 1; n--) {
				if (numCellsFilled + n > numCells) continue;
				List<Mask> list = masks.get(n);
				for (Mask mask : list) {
					if (s.used.contains(mask)) continue;
					BitSet overlap = new BitSet();
					overlap.or(s.grid);
					overlap.and(mask.bits);
					if (overlap.isEmpty()) {
						BitSet newGrid = new BitSet();
						newGrid.or(s.grid);
						newGrid.or(mask.bits);
						Set<Mask> newUsed = new HashSet<Mask>();
						newUsed.addAll(s.used);
						newUsed.add(mask);
						int priority = newGrid.cardinality() - numCellsFilled;
						queue.add(new State(newGrid, newUsed, priority, weight));
					}
				}
			}
		}
	}
}
