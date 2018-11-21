/*
 * Copyright (c) 2017 knilch - freeshisen@cwde.de
 * Copyright (c) 2013 contact.proofofconcept@gmail.com
 *
 * Licensed under GNU General Public License, version 2 or later.
 * Some rights reserved. See COPYING.
 */

package de.cwde.freeshisen;

class Line {
    final Point a;
    final Point b;
    private final boolean isHorizontal;
    private final boolean isVertical;
    private final Point Min;
    private final Point Max;

    Line(Point a, Point b) {
        this.a = a;
        this.b = b;
        isHorizontal = (a.i == b.i);
        isVertical = (a.j == b.j);
        if (a.i < b.i || a.j < b.j) {
            Min = a;
            Max = b;
        } else {
            Min = b;
            Max = a;
        }
    }

    boolean equals(Line l) {
        return (a.equals(l.a) && b.equals(l.b));
    }

    boolean contains(Point p) {
        return (isHorizontal && p.i == a.i && p.j >= Min.j && p.j <= Max.j)
                || (isVertical && p.j == a.j && p.i >= Min.i && p.i <= Max.i);
    }

    Point cuts(Line l) {
        if (isHorizontal && l.isVertical
                && Min.j <= l.a.j && Max.j >= l.a.j
                && l.Min.i <= a.i && l.Max.i >= a.i) {
            return new Point(a.i, l.a.j);
        } else if (isVertical && l.isHorizontal
                && Min.i <= l.a.i && Max.i >= l.a.i
                && l.Min.j <= a.j && l.Max.j >= a.j) {
            return new Point(l.a.i, a.j);
        } else return null;
    }

    boolean isPoint() {
        return (a.i == b.i && a.j == b.j);
    }
}
