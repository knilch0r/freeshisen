package de.cwde.freeshisen;

import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ShisenSho extends Application {	
	private static ShisenSho instance = null; 
	private ShisenShoView view = null;
	public ShisenShoActivity activity = null;

	public Board board;
	public int[] boardSize=new int[2];
	public int difficulty=1; // 1=Easy, 2=Hard
	public int size=3; // 1=Small, 2=Medium, 3=Big
	public String tilesetid = "classic";
	public boolean gravity=true;
	public boolean timeCounter=true;

	public void newPlay() {
		board = new Board();
		board.buildRandomBoard(boardSize[0],boardSize[1],difficulty,gravity);
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

	public void sleep(int deciSeconds) {
		try {
			Thread.sleep(deciSeconds*100);
		} catch (InterruptedException e) { }
	}

	public ShisenSho() {
		instance = this;
		setSize(size);
	}

	public static synchronized ShisenSho app() {
		return instance;
	}

	public ShisenShoView getView() {
		if (view == null) view = new ShisenShoView(this);
		return view;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate() {
		super.onCreate();
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		Log.d("ShisenSho", "starting up...");
		loadOptions();
	}

	private void loadOptions() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

		// FIXME: handle NumberFormatException here?
		setSize(Integer.parseInt(sp.getString("pref_size", "1")));
		difficulty = Integer.parseInt(sp.getString("pref_diff", "1"));
		gravity = sp.getBoolean("pref_grav", true);
		timeCounter = sp.getBoolean("pref_time", true);
		tilesetid = sp.getString("pref_tile", "");
	}

	public void checkForChangedOptions() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

		// FIXME: handle NumberFormatException here?
		int size = Integer.parseInt(sp.getString("pref_size", "1"));
		int difficulty = Integer.parseInt(sp.getString("pref_diff", "1"));
		boolean gravity = sp.getBoolean("pref_grav", true);
		boolean timeCounter = sp.getBoolean("pref_time", true);
		String tilesetid = sp.getString("pref_tile", "");

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

		if ((tilesetid != this.tilesetid) && (view != null)) {
			// tileset can be changed without a reset
			this.tilesetid = tilesetid;
			view.loadTileset();
		}

		if (needsReset && (view != null) && (activity != null)) {
			new AlertDialog.Builder(this)
				.setTitle("Preferences changed!") // FIXME: hardcoded string
				.setCancelable(true)
				.setIcon(R.drawable.icon)
				.setPositiveButton(android.R.string.yes,
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User clicked OK button - reset game
						((ShisenSho) ((AlertDialog) dialog).getContext()).view.reset();
					}
				})
				.setNegativeButton(android.R.string.no, null)
				.setMessage("Changes in Preferences will only have an effect if" +
							" a new game is started. Abort current game and start" +
							" a new one?").create() // FIXME: hardcoded string
				.show();
		} else {
			Log.d("ShisenSho", "Preferences changed, but no view or activity online - huh?");
		}

	}
}
