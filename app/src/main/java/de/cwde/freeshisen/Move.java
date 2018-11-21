/*
 * Copyright (c) 2017 knilch - freeshisen@cwde.de
 * Copyright (c) 2013 contact.proofofconcept@gmail.com
 *
 * Licensed under GNU General Public License, version 2 or later.
 * Some rights reserved. See COPYING.
 */

package de.cwde.freeshisen;

class Move {
    final Point a;
    final Point b;
    final char piece;

    Move(Point a, Point b, char piece) {
        this.a = a;
        this.b = b;
        this.piece = piece;
    }
}
