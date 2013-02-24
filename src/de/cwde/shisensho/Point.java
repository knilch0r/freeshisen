package de.cwde.shisensho;

class Point {
	public Point(int i, int j) {
		this.i=i;
		this.j=j;
	}

	public boolean equals(Point p) {
		return (i==p.i && j==p.j);
	}

	public String toString() {
		return "("+i+","+j+")";
	}

	public static Point fromString(String s) {
		String[] ij=s.split(",",2);
		int i=Integer.parseInt(ij[0]);
		int j=Integer.parseInt(ij[1]);
		return new Point(i,j);
	}

	public int i;
	public int j;

	public Point copy() {
		return new Point(this.i,this.j);
	}
}
