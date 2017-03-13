package de.cwde.freeshisen;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ShisenSho extends Application {
	private static ShisenSho instance = null;
	public ShisenShoActivity activity = null;
	public Board board;
	public int[] boardSize = new int[2];
	public int difficulty = 1; // 1=Easy, 2=Hard
	public int size = 3; // 1=Small, 2=Medium, 3=Big
	public String tilesetId = "classic";
	public boolean gravity = true;
	public boolean timeCounter = true;
	private ShisenShoView view = null;

	public ShisenSho() {
		instance = this;
		setSize(size);
	}

	public void setSize(int s) {
		size = s;

		switch (s) {
			case 1:
				boardSize[0] = 6 + 2;
				boardSize[1] = 8 + 2;
				break;
			case 2:
				boardSize[0] = 6 + 2;
				boardSize[1] = 12 + 2;
				break;
			case 3:
			default:
				boardSize[0] = 6 + 2;
				boardSize[1] = 16 + 2;
				break;
		}
	}

	public static synchronized ShisenSho app() {
		return instance;
	}

	public void newPlay() {
		loadOptions();
		board = new Board();
		board.buildRandomBoard(boardSize[0], boardSize[1], difficulty, gravity);
	}

	private void loadOptions() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

		int s = 1;
		int d = 1;
		try {
			s = Integer.parseInt(sp.getString("pref_size", "1"));
			d = Integer.parseInt(sp.getString("pref_diff", "1"));
		} catch (NumberFormatException e) {
			// we'll use the defaults we set earlier
		}

		setSize(s);
		difficulty = d;
		gravity = sp.getBoolean("pref_grav", true);
		timeCounter = sp.getBoolean("pref_time", true);
		tilesetId = sp.getString("pref_tile", "");
	}

	public void sleep(int deciSeconds) {
		try {
			Thread.sleep(deciSeconds * 100);
		} catch (InterruptedException e) {
		}
	}

	public synchronized ShisenShoView getView() {
		if (view == null) view = new ShisenShoView(this);
		return view;
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		Log.d("ShisenSho", "starting up...");
		loadOptions();
	}

	public void checkForChangedOptions() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

		// FIXME: handle NumberFormatException here?
		int size = Integer.parseInt(sp.getString("pref_size", "1"));
		int difficulty = Integer.parseInt(sp.getString("pref_diff", "1"));
		boolean gravity = sp.getBoolean("pref_grav", true);
		boolean timeCounter = sp.getBoolean("pref_time", true);
		String tilesetid = sp.getString("pref_tile", "");
//		boolean usesound = sp.getBoolean("pref_sound", false);

		boolean needsReset = false;

		if (size != this.size) {
			needsReset = true;
		}

		if (difficulty != this.difficulty) {
			needsReset = true;
		}

		if (gravity != this.gravity) {
			needsReset = true;
		}

		if (timeCounter != this.timeCounter) {
			needsReset = true;
		}

		if ((tilesetid != this.tilesetId) && (view != null)) {
			// tileset can be changed without a reset
			this.tilesetId = tilesetid;
			view.loadTileset();
		}

		if (needsReset) {
			if ((view != null) && (activity != null)) {
				view.onOptionsChanged();
			} else {
				Log.d("ShisenSho", "Preferences changed, but no view or activity online - huh?");
			}
		}

	}
}
