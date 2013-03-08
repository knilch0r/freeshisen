package de.cwde.freeshisen;

class Point {
	public Point(int i, int j) {
		this.i = i;
		this.j = j;
	}

	public Point(Point p) {
		this.i = p.i;
		this.j = p.j;
	}

	public boolean equals(Point p) {
		return (i == p.i && j == p.j);
	}

	public boolean equals(int myi, int myj) {
		return (i == myi && j == myj);
	}

	public String toString() {
		return "(" + i + "," + j + ")";
	}

	public void set(int i, int j) {
		this.i = i;
		this.j = j;
	}

	public static Point fromString(String s) {
		String[] ij = s.split(",", 2);
		int i = Integer.parseInt(ij[0]);
		int j = Integer.parseInt(ij[1]);
		return new Point(i, j);
	}

	public int i;
	public int j;

	public Point copy() {
		return new Point(this.i, this.j);
	}
}
