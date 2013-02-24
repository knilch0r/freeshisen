package de.cwde.shisensho;

public class Line {
	public Line(Point a, Point b) {
		this.a=a;                    
		this.b=b;                    
	}                              

	public boolean equals(Line l) {   
		return (a.equals(l.a) && b.equals(l.b));
	}                                         

	public boolean isHorizontal() {             
		return (a.i==b.i);                      
	}                                         

	public boolean isVertical() {               
		return (a.j==b.j);                      
	}                                         

	public boolean contains(Point p) {
		return (p.i==a.i && p.i==b.i && p.j>=getMin().j && p.j<=getMax().j)
		|| (p.j==a.j && p.j==b.j && p.i>=getMin().i && p.i<=getMax().i);
	}

	public Point cuts(Line l) {
		if (isHorizontal() && l.isVertical()
				&& getMin().j<=l.a.j && getMax().j>=l.a.j
				&& l.getMin().i<=a.i && l.getMax().i>=a.i ) {
			return new Point(a.i,l.a.j);
		} else if (isVertical() && l.isHorizontal()
				&& getMin().i<=l.a.i && getMax().i>=l.a.i
				&& l.getMin().j<=a.j && l.getMax().j>=a.j ) {
			return new Point(l.a.i,a.j);
		} else return null;
	}

	public Point getMin() {
		if (a.i<b.i || a.j<b.j) return a;
		else return b;
	}

	public Point getMax() {
		if (a.i>b.i || a.j>b.j) return a;
		else return b;
	}

	public String toString() {
		return a+"-"+b;
	}

	public Point a;
	public Point b;
}
