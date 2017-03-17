/*
 * Copyright (c) 2017 knilch - freeshisen@cwde.de
 * Copyright (c) 2013 contact.proofofconcept@gmail.com
 *
 * Licensed under GNU General Public License, version 2 or later.
 * Some rights reserved. See COPYING.
 */

package de.cwde.freeshisen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class Board {
	private static final String charpieces = " abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	char[][] board;
	int boardSizeY = 0;
	int boardSizeX = 0;
	private boolean gravity = true;
	private LinkedList<Move> history;

	Board() {
	}

	static String pieceToString(char piece) {
		return charpieces.substring(piece, 1);
	}

	public String toString() {
		String result = "  ";
		for (int j = 0; j < boardSizeX; j++) {
			if (j > 0) result += " ";
			result += "" + (j % 10);
		}
		result += "\n  " + StringRepeat("--", boardSizeX);
		for (int i = 0; i < boardSizeY; i++) {
			result += "\n" + (i % 10) + "|";
			for (int j = 0; j < boardSizeX; j++) {
				if (j > 0) result += " ";
				result += charpieces.substring(board[i][j], board[i][j] + 1);
			}
			result += " |\n";
			if (i < boardSizeY - 1)
				result += " |" + StringRepeat("  ", boardSizeX) + "|";
		}
		result += "  " + StringRepeat("--", boardSizeX) + "\n";
		return result;
	}

	private String StringRepeat(String s, int n) {
		String result = "";
		for (int i = 0; i < n; i++)
			result += s;
		return result;
	}

	void buildRandomBoard(int sizeI, int sizeJ, int difficulty, boolean gravity) {
		initialize(sizeI, sizeJ);
		this.gravity = gravity;

		int numDifferentPieces = ((boardSizeY - 2) * (boardSizeX - 2) / ((4 - difficulty) * 2)) + 1;
		for (int n = 0; n < ((4 - difficulty) * 2); n++) {
			for (int k = 0; k < numDifferentPieces; k++) {
				int i, j;
				do {
					j = (myrand() % (boardSizeX - 2)) + 1;
					i = findFreeRow(j);
				} while (i < 1);
				board[i][j] = (char) k;
			}
		}
	}

	private void initialize(int sizeI, int sizeJ) {
		boardSizeY = sizeI;
		boardSizeX = sizeJ;
		board = new char[boardSizeY][boardSizeX];
		for (int i = 0; i < boardSizeY; i++)
			for (int j = 0; j < boardSizeX; j++)
				board[i][j] = 0;
		history = new LinkedList<>();
	}

	/* RAND_MAX assumed to be 32767 */
	private int myrand() {
		return (int) Math.floor(Math.random() * 32768);
	}

	private int findFreeRow(int j) {
		for (int i = 1; i < boardSizeY - 1; i++) {
			if (board[i][j] != 0) return (i - 1);
		}
		return (boardSizeY - 1 - 1);
	}

	void play(Point a0, Point b0) {
		// It's important to sink the upper piece first
		Point a = (a0.i < b0.i) ? a0 : b0;
		Point b = (a0.i < b0.i) ? b0 : a0;
		Move m = new Move(a, b, getPiece(a));
		history.add(0, m);
		setPiece(a, (char) 0);
		processGravity(a);
		setPiece(b, (char) 0);
		processGravity(b);
	}

	private char getPiece(Point p) {
		return board[p.i][p.j];
	}

	private void setPiece(Point p, char piece) {
		board[p.i][p.j] = piece;
	}

	private void processGravity(Point p) {
		if (gravity) for (int i = p.i; i > 0; i--) board[i][p.j] = board[i - 1][p.j];
	}

	void undo() {
		if (!getCanUndo()) return;
		Move m = history.remove(0);
		undoGravity(m.b);
		setPiece(m.b, m.piece);
		undoGravity(m.a);
		setPiece(m.a, m.piece);
	}

	boolean getCanUndo() {
		return !history.isEmpty();
	}

	private void undoGravity(Point p) {
		if (gravity) for (int i = 0; i < p.i; i++) board[i][p.j] = board[i + 1][p.j];
	}

	Line getNextPair() {
		Line result = null;
		List<Integer> pieces = new ArrayList<>();
		List<List<Point>> piecePoints = new ArrayList<>();
		for (int i = 0; i < boardSizeY; i++) {
			for (int j = 0; j < boardSizeX; j++) {
				int piece = (int) board[i][j];
				if (piece == 0) continue;
				int key = pieces.indexOf(piece);
				Point p = new Point(i, j);
				if (key == -1) {
					List<Point> points0 = new ArrayList<>();
					points0.add(p);
					pieces.add(piece);
					piecePoints.add(points0);

					key = pieces.indexOf(piece);
					piecePoints.get(key);
				} else {
					List<Point> points1 = piecePoints.get(key);
					points1.add(p);
				}
			}
		}

		for (List<Point> points : piecePoints) {
			int n = points.size();
			for (int i = 0; i < n; i++) {
				Point a = points.get(i);
				for (int j = i + 1; j < n; j++) {
					Point b = points.get(j);
					List<Point> path = getPath(a.copy(), b.copy());
					if (path != null && path.size() > 0) {
						result = new Line(a, b);
						break;
					}
				}
				if (result != null) break;
			}
			if (result != null) break;
		}
		return result;
	}

	/*
	  ALGORITHM TO COMPUTE CONNECTION PATH BETWEEN PIECES A (IA,JA) AND B (IB,JB)

	  - Delete A and B from the board (consider them as blank spaces)
	  - Calculate the set H of possible horizontal lines in the board (lines through blank spaces)
	  - Calculate the set V of possible vertical lines in the board
	  - Find HA, VA, HB, VB in the sets
	  - If HA=HB, result is a straight horizontal line A-B
	  - If VA=VB, result is a straight vertical line A-B
	  - If HA cuts VB, the result is an L line A-(IA,JB)-B
	  - If VA cuts HB, the result is an L line A-(IB,JA)-B
	  - If exists an V line that cuts HA and HB, the result is a Z line A-(IA,JV)-(IB-JV)-B
	  - If exists an H line that cuts VA and VB, the result is a Z line A-(IV,JA)-(IV,JB)-B
	 */
	List<Point> getPath(Point a, Point b) {
		List<Point> result = new ArrayList<>();

		if (getPiece(a) != getPiece(b)) return result;

		List<Line> h = getHorizontalLines(a, b);
		List<Line> v = getVerticalLines(a, b);
		Line ha = null, va = null, hb = null, vb = null;

		for (Line l : h) {
			if (l.contains(a)) ha = l;
			if (l.contains(b)) hb = l;
			if (ha != null && hb != null) break;
		}

		for (Line l : v) {
			if (l.contains(a)) va = l;
			if (l.contains(b)) vb = l;
			if (va != null && vb != null) break;
		}

		if ((ha == null) || (va == null) || (hb == null) || (vb == null)) {
			// actually, the way get*Lines work, these will never be null: each point is
			// at least included in a "Line" of length 0
			return result;
		}
		if ((ha.isPoint() && va.isPoint()) || (hb.isPoint() && vb.isPoint())) {
			// either a or b don't have any connections
			return result;
		}

		if (ha.equals(hb) || va.equals(vb)) {
			result.add(a);
			result.add(b);
			return result;
		}

		Point ab;

		ab = ha.cuts(vb);

		if (ab != null) {
			result.add(a);
			result.add(ab);
			result.add(b);
			return result;
		}

		ab = va.cuts(hb);

		if (ab != null) {
			result.add(a);
			result.add(ab);
			result.add(b);
			return result;
		}

		for (Line l : v) {
			Point al = l.cuts(ha);
			Point bl = l.cuts(hb);

			if (al != null && bl != null) {
				result.add(a);
				result.add(al);
				result.add(bl);
				result.add(b);
				return result;
			}
		}

		for (Line l : h) {
			Point al = l.cuts(va);
			Point bl = l.cuts(vb);

			if (al != null && bl != null) {
				result.add(a);
				result.add(al);
				result.add(bl);
				result.add(b);
				return result;
			}
		}

		return result;
	}

	@SuppressWarnings("ConstantConditions")
	private List<Line> getHorizontalLines(Point excludeA, Point excludeB) {
		List<Line> result = new ArrayList<>();
		for (int i = 0; i < boardSizeY; i++) {
			int j0 = -1;
			boolean empty;
			for (int j = 0; j < boardSizeX; j++) {
				empty = (board[i][j] == 0 || (i == excludeA.i && j == excludeA.j)
						|| (i == excludeB.i && j == excludeB.j));
				if (j0 == -1 && empty) {
					j0 = j;
				} else if (j0 != -1 && !empty) {
					result.add(new Line(new Point(i, j0), new Point(i, j - 1)));
					j0 = -1;
				}
			}
			if (j0 != -1) result.add(new Line(new Point(i, j0), new Point(i, boardSizeX - 1)));
		}

		return result;
	}

	@SuppressWarnings("ConstantConditions")
	private List<Line> getVerticalLines(Point excludeA, Point excludeB) {
		List<Line> result = new ArrayList<>();
		for (int j = 0; j < boardSizeX; j++) {
			int i0 = -1;
			boolean empty;
			for (int i = 0; i < boardSizeY; i++) {
				empty = (board[i][j] == 0 || (i == excludeA.i && j == excludeA.j)
						|| (i == excludeB.i && j == excludeB.j));
				if (i0 == -1 && empty) {
					i0 = i;
				} else if (i0 != -1 && !empty) {
					result.add(new Line(new Point(i0, j), new Point(i - 1, j)));
					i0 = -1;
				}
			}
			if (i0 != -1) result.add(new Line(new Point(i0, j), new Point(boardSizeY - 1, j)));
		}

		return result;
	}

	int getNumPieces() {
		int result = 0;
		for (int j = 0; j < boardSizeX; j++) {
			for (int i = 0; i < boardSizeY; i++) {
				if (board[i][j] != 0) result++;
			}
		}
		return result;
	}

}
