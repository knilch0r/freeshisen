package de.cwde.shisensho;

import android.app.Application;
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
	public boolean gravity=true;
	public boolean timeCounter=true;

	public static void log(String msg) {
		Log.w("ShisenSho", msg);
	}

	public void newPlay() {
		board = new Board();
		board.buildRandomBoard(boardSize[0],boardSize[1],difficulty,gravity);
	}

	public void setSize(int s) {
		switch (s) {
		case 1:
			size=1;
			boardSize[0]=6+2;
			boardSize[1]=8+2;
			break;
		case 2:
			size=2;
			boardSize[0]=6+2;
			boardSize[1]=12+2;
			break;
		case 3:
		default:
			size=3;
			boardSize[0]=6+2;
			boardSize[1]=16+2;
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
		setOptions();
	}

	public void setOptions() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

		int size = sharedPref.getInt("size", 1);
		int difficulty = sharedPref.getInt("difficulty", 1);
		boolean gravity = sharedPref.getBoolean("gravity", true);
		boolean timeCounter = sharedPref.getBoolean("timeCounter", true);

		boolean needsReset = false;

		if (size != this.size) {
			setSize(size);
			needsReset = true;
		}

		if (difficulty != this.difficulty) {
			this.difficulty = difficulty;
			needsReset = true;
		}

		if (gravity != this.gravity) {
			this.gravity = gravity;
			needsReset = true;
		}

		if (timeCounter != this.timeCounter) {
			this.timeCounter = timeCounter;
			view.onTimeCounterActivate();
		}

		if (needsReset && (view != null)) {
			view.reset();
		}
	}

}
