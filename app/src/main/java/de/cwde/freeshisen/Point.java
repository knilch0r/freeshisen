package de.cwde.freeshisen;

class Point {
	public Point(int i, int j) {
		this.i = i;
		this.j = j;
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

	public int i;
	public int j;

	public Point copy() {
		return new Point(this.i, this.j);
	}
}
