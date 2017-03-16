/*
 * Copyright (c) 2017 knilch - freeshisen@cwde.de
 * Copyright (c) 2013 contact.proofofconcept@gmail.com
 *
 * Licensed under GNU General Public License, version 2 or later.
 * Some rights reserved. See COPYING.
 */

package de.cwde.freeshisen;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

public class Tileset {
	public int tileWidth;
	public int tileHeight;
	public Bitmap[] tile;
	private ShisenSho app;

	public Tileset(ShisenSho shishenSho) {
		this.app = shishenSho;
	}

	public void loadTileset(int screenWidth, int screenHeight) {
		boolean isSVG = false;
		int id;
		String s = app.tilesetId;

		if (s.equals("classic")) {
			id = R.drawable.classic;
		} else if (s.equals("jade")) {
			id = R.drawable.jade;
		} else if (s.equals("traditional")) {
			id = R.drawable.traditional;
		} else if (s.equals("pixel")) {
			id = R.drawable.pixel;
		} else if (s.equals("original")) {
			id = R.drawable.original;
		} else if (s.equals("veit")) {
			id = R.drawable.veit;
		} else {
			// shouldn't be reached...
			Log.e("ShisenSho", "somebody managed to set an invalid tileset string");
			id = R.drawable.original;
		}

		if (isSVG) {
			loadSVGTileset(id, screenWidth, screenHeight);
		} else {
			loadPNGTileset(id, screenWidth, screenHeight);
		}
	}

	private void loadSVGTileset(int tilesetid, int screenWidth, int screenHeight) {
		// TODO
	}

	private void loadPNGTileset(int tilesetid, int screenWidth, int screenHeight) {
		BitmapFactory.Options ops = new BitmapFactory.Options();
		ops.inScaled = false;
		Bitmap tileset = BitmapFactory.decodeResource(app.getResources(), tilesetid, ops);
		tileset.setDensity(Bitmap.DENSITY_NONE);

		// The tile set has 4 rows x 9 columns
		int tilesetRows = 4;
		int tilesetCols = 9;
		int loadedtileWidth = tileset.getWidth() / tilesetCols;
		int loadedtileHeight = tileset.getHeight() / tilesetRows;
		tile = new Bitmap[tilesetRows * tilesetCols];

		// align to screen:
		// "large" is 16x6, and we want to have a nice border, so we use 17x7 and
		// choose the lowest scale so everything fits
		float scalex = ((float) (screenWidth - 2) / 17) / loadedtileWidth;
		float scaley = ((float) (screenHeight - 2) / 7) / loadedtileHeight;
		if (scaley < scalex) {
			scalex = scaley;
		} else {
			scaley = scalex;
		}
		Matrix matrix = new Matrix();
		matrix.setScale(scalex, scaley);

		int k = 0;
		for (int i = 0; i < tilesetRows; i++) {
			for (int j = 0; j < tilesetCols; j++) {
				tile[k] = Bitmap.createBitmap(tileset, j * loadedtileWidth, i * loadedtileHeight,
						loadedtileWidth, loadedtileHeight, matrix, false);
				tile[k].setDensity(Bitmap.DENSITY_NONE);
				k++;
			}
		}
		tileWidth = tile[0].getWidth();
		tileHeight = tile[0].getHeight();
	}
}
