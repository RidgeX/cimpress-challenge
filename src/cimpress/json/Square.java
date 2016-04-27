package cimpress.json;

public class Square implements Comparable<Square> {
	public int X;
	public int Y;
	public int Size;
	
	public Square(int x, int y, int size) {
		X = x;
		Y = y;
		Size = size;
	}
	
	public int compareTo(Square square) {
		int d = Y - square.Y;
		if (d != 0) return d;
		return X - square.X;
	}
	
	public String toString() {
		return String.format("(%d,%d,%d)", X, Y, Size);
	}
}
