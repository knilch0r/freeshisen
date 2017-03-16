/*
 * Copyright (c) 2017 knilch - freeshisen@cwde.de
 * Copyright (c) 2013 contact.proofofconcept@gmail.com
 *
 * Licensed under GNU General Public License, version 2 or later.
 * Some rights reserved. See COPYING.
 */

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
