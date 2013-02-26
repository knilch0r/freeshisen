package de.cwde.shisensho;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Board {
	private static String charpieces = " abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	public int difficulty=1; // 1=Hard ... N=Easy
	public boolean gravity=true;
	public int [] boardSize;
	public char[][] board;
	public LinkedList<Move> history;

	// ----------------------
	// Public methods
	// ----------------------

	public Board() {
	}

	// The board always has a 1-square width free rectangle that has
	// to be taken into account when specifying the size
	public void initialize(int sizeI, int sizeJ) {
		boardSize = new int[2];
		boardSize[0]=sizeI;
		boardSize[1]=sizeJ;
		board = new char[boardSize[0]][boardSize[1]];
		for (int i=0;i<boardSize[0];i++)
			for (int j=0;j<boardSize[1];j++)
				board[i][j]=0;
		history=new LinkedList<Move>();
	}

	public static String pieceToString(char piece) {
		return charpieces.substring(piece,1);
	}

	public static char StringToPiece(String piece) {
		char upiece;
		long charpiecesLen=charpieces.length();
		for(upiece=0;(upiece<charpiecesLen && charpieces.substring(upiece,1)!=piece);upiece++);
		if (upiece<charpiecesLen) return upiece;
		else return 0;
	}

	public String toString() {
		String result="  ";
		for (int j=0;j<boardSize[1];j++) {
			if (j>0) result+=" ";
			result+=""+(j%10);
		}
		result+="\n  "+StringRepeat("--",boardSize[1]);
		for (int i=0;i<boardSize[0];i++) {
			result+="\n"+(i%10)+"|";
			for (int j=0;j<boardSize[1];j++) {
				if (j>0) result+=" ";
				result+=charpieces.substring(board[i][j],board[i][j]+1);
			}
			result+=" |\n";
			if (i<boardSize[0]-1)
				result+=" |"+StringRepeat("  ",boardSize[1])+"|";
		}
		result+="  "+StringRepeat("--",boardSize[1])+"\n";
		return result;
	}

	public void buildRandomBoard(int sizeI, int sizeJ, int difficulty, boolean gravity) {
		initialize(sizeI,sizeJ);
		this.difficulty=difficulty;
		this.gravity=gravity;

		int numDifferentPieces=((boardSize[0]-2)*(boardSize[1]-2)/((difficulty+1)*2))+1;
		for (int n=0;n<((difficulty+1)*2);n++) {
			for (int k=0;k<numDifferentPieces;k++) {
				int i,j;
				do {
					j=(myrand() % (boardSize[1]-2))+1;
					i=findFreeRow(j);
				} while (i<1);
				// ShisenSho.log("numDifferentPieces="+numDifferentPieces+", n="+n+", k="+(int)k+", i="+i+", j="+j);
				// ShisenSho.log(toString());
				board[i][j]=(char)k;
			}
		}
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

	  The following data types are defined:

	  - Board
	  - Point(int i, int j)
	  - Line(Point a, Point b)
	  - LineSet(Line l1, ..., Line lN)

	  The following operations are defined

	  - LineSet getHorizontalLines(Board board, Point a, Point b) // a and b needed to consider them as blank
	  - LineSet getVerticalLines(Board board, Point a, Point b)
	  - boolean lineIsHorizontal(Line l)
	  - boolean lineIsVertical(Line l)
	  - boolean lineContainsPoint(Line l, Point p)
	  - boolean lineEqualsLine(Line l1, Line l2)
	  - boolean lineCutsLine(Line l1, Line l2)
	 */
	public List<Point> getPath(Point a, Point b) {
		List<Point> result=new ArrayList<Point>();

		if (getPiece(a)!=getPiece(b)) return result;

		List<Line> h=getHorizontalLines(a,b);
		List<Line> v=getVerticalLines(a,b);
		Line ha=null, va=null, hb=null, vb=null;

		for (Line l : h) {
			if (l.contains(a)) ha=l;
			if (l.contains(b)) hb=l;
			if (ha!=null && hb!=null) break;
		}

		for (Line l : v) {
			if (l.contains(a)) va=l;
			if (l.contains(b)) vb=l;
			if (va!=null && vb!=null) break;
		}

		// stdout.printf("va=%s, ha=%s, vb=%s, hb=%s\n",va.toString(),ha.toString(),vb.toString(),hb.toString());

		if ((ha==null && va==null) || (hb==null && vb==null))
			return result;

		if (ha.equals(hb) || va.equals(vb)) {
			result.add(a);
			result.add(b);
			return result;
		}

		Point ab;

		ab=ha.cuts(vb);
		// stdout.printf("(ha cuts vb) ab=%s\n",ab.toString());

		if (ab!=null) {
			result.add(a);
			result.add(ab);
			result.add(b);
			return result;
		}

		ab=va.cuts(hb);
		// stdout.printf("(va cuts hb) ab=%s\n",ab.toString());

		if (ab!=null) {
			result.add(a);
			result.add(ab);
			result.add(b);
			return result;
		}

		for (Line l : v) {
			Point al=l.cuts(ha);
			Point bl=l.cuts(hb);

			// stdout.printf("(%s cuts ha) al=%s\n",l.toString(),al.toString());
			// stdout.printf("(%s cuts hb) bl=%s\n",l.toString(),bl.toString());

			if (al!=null && bl!=null) {
				result.add(a);
				result.add(al);
				result.add(bl);
				result.add(b);
				return result;
			}
		}

		for (Line l : h) {
			Point al=l.cuts(va);
			Point bl=l.cuts(vb);

			// stdout.printf("(%s cuts va) al=%s\n",l.toString(),al.toString());
			// stdout.printf("(%s cuts vb) bl=%s\n",l.toString(),bl.toString());

			if (al!=null && bl!=null) {
				result.add(a);
				result.add(al);
				result.add(bl);
				result.add(b);
				return result;
			}
		}

		return result;
	}

	public char getPiece(Point p) {
		return board[p.i][p.j];
	}

	public void setPiece(Point p, char piece) {
		board[p.i][p.j]=piece;
	}

	public String getStrPiece(Point p) {
		char piece=board[p.i][p.j];
		return charpieces.substring(piece,1);
	}

	public void setStrPiece(Point p, String piece) {
		char upiece;
		long charpiecesLen=charpieces.length();
		for(upiece=0;(upiece<charpiecesLen && charpieces.substring(upiece,1)!=piece);upiece++);
		if (upiece<charpiecesLen) board[p.i][p.j]=upiece;
	}

	public void play(Point a0, Point b0) {
		// It's important to sink the upper piece first
		Point a=(a0.i<b0.i)?a0:b0;
		Point b=(a0.i<b0.i)?b0:a0;
		Move m=new Move(a,b,getPiece(a));
		history.add(0,m);
		setPiece(a,(char)0);
		processGravity(a);
		setPiece(b,(char)0);
		processGravity(b);
	}

	public boolean getCanUndo() {
		return !history.isEmpty();
	}

	public void undo() {
		if (!getCanUndo()) return;
		Move m=history.remove(0);
		undoGravity(m.b);
		setPiece(m.b,m.piece);
		undoGravity(m.a);
		setPiece(m.a,m.piece);
	}

	public List<Line> getPairs(int maxResults) {
		List<Line> result=new ArrayList<Line>();
		List<Integer> pieces=new ArrayList<Integer>();
		List<List<Point>> piecePoints=new ArrayList<List<Point>>();
		for (int i=0;i<boardSize[0];i++)
			for (int j=0;j<boardSize[1];j++) {
				int piece=(int)board[i][j];
				if (piece==0) continue;
				int key=pieces.indexOf(piece);
				Point p=new Point(i,j);
				if (key==-1) {
					List<Point> points0=new ArrayList<Point>();
					points0.add(p);
					pieces.add(piece);
					piecePoints.add(points0);

					key=pieces.indexOf(piece);
					piecePoints.get(key);
				} else {
					List<Point> points1=piecePoints.get(key);
					points1.add(p);
				}
			}

		int nresults=0;
		for (List<Point> points : piecePoints) {
			int n=(int)points.size();
			for (int i=0;i<n;i++) {
				Point a=points.get(i);
				for (int j=i+1;j<n;j++) {
					Point b=points.get(j);
					List<Point> path=getPath(a.copy(),b.copy());
					if (path!=null && path.size()>0) {
						result.add(new Line(a,b));
						if (nresults++==maxResults) break;
					}
				}
				if (nresults==maxResults) break;
			}
			if (nresults==maxResults) break;
		}
		return result;
	}

	public int getNumPieces() {
		int result=0;
		for (int j=0;j<boardSize[1];j++) {
			for (int i=0;i<boardSize[0];i++) {
				if (board[i][j]!=0) result++;
			}
		}
		return result;
	}

	// ----------------------
	// Private methods
	// ----------------------

	/* RAND_MAX assumed to be 32767 */
	private int myrand() {
		return (int)Math.floor(Math.random()*32768);
	}

	private String StringRepeat(String s, int n) {
		String result="";
		for (int i=0;i<n;i++)
			result+=s;
		return result;
	}

	private int findFreeRow(int j) {
		for (int i=1;i<boardSize[0]-1;i++) {
			if (board[i][j]!=0) return (i-1);
		}
		return (boardSize[0]-1-1);
	}

	private List<Line> getHorizontalLines(Point excludeA, Point excludeB) {
		List<Line> result=new ArrayList<Line>();
		for (int i=0;i<boardSize[0];i++) {
			int j0=-1;
			boolean empty;
			for (int j=0;j<boardSize[1];j++) {
				empty=(board[i][j]==0 || (i==excludeA.i && j==excludeA.j)
						|| (i==excludeB.i && j==excludeB.j));
				if (j0==-1 && empty) {
					j0=j;
				} else if (j0!=-1 && !empty) {
					result.add(new Line(new Point(i,j0), new Point(i,j-1)));
					j0=-1;
				}
			}
			if (j0!=-1) result.add(new Line(new Point(i,j0), new Point(i,boardSize[1]-1)));
		}

		// stdout.printf("\ngetHorizontalLines( %s, %s ): ",excludeA.toString(),excludeB.toString());
		// for (Line line : result) stdout.printf("%s ",line.toString());
		// stdout.printf("\n");

		return result;
	}

	private List<Line> getVerticalLines(Point excludeA, Point excludeB) {
		List<Line> result=new ArrayList<Line>();
		for (int j=0;j<boardSize[1];j++) {
			int i0=-1;
			boolean empty;
			for (int i=0;i<boardSize[0];i++) {
				empty=(board[i][j]==0 || (i==excludeA.i && j==excludeA.j)
						|| (i==excludeB.i && j==excludeB.j));
				if (i0==-1 && empty) {
					i0=i;
				} else if (i0!=-1 && !empty) {
					result.add(new Line(new Point(i0,j), new Point(i-1,j)));
					i0=-1;
				}
			}
			if (i0!=-1) result.add(new Line(new Point(i0,j), new Point(boardSize[0]-1,j)));
		}

		// stdout.printf("\ngetVerticalLines( %s, %s ): ",excludeA.toString(),excludeB.toString());
		// for (Line line : result) stdout.printf("%s ",line.toString());
		// stdout.printf("\n");

		return result;
	}

	private void processGravity(Point p) {
		if (gravity) for (int i=p.i;i>0;i--) board[i][p.j]=board[i-1][p.j];
	}

	private void undoGravity(Point p) {
		if (gravity) for (int i=0;i<p.i;i++) board[i][p.j]=board[i+1][p.j];
	}

}
