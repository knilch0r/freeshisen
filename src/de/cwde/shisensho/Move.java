package de.cwde.shisensho;

public class Move {
	public Move(Point a, Point b, char piece) {
		this.a=a;
		this.b=b;
		this.piece=piece;
	}

	public String toString() {
		return a+"-"+b+"("+Board.pieceToString(piece)+")";
	}

	public Point a;
	public Point b;
	public char piece;
}
