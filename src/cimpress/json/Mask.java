package cimpress.json;

import java.util.*;

public class Mask {
	public Square square;
	public BitSet bits;
	
	public Mask(Square square, BitSet bits) {
		this.square = square;
		this.bits = bits;
	}
	
	public String toString() {
		return square.toString();
	}
}
