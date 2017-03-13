package de.cwde.freeshisen;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.abs;

class ShisenShoView extends SurfaceView implements SurfaceHolder.Callback {

	private static final String INVALID_TIME = "9:99:99";
	private static final String COLOR_TEXT = "#FFFFFF";
	private static final String COLOR_TEXT_SHADOW = "#000000";
	private static final String COLOR_HINT = "#F0C000";
	private static final String COLOR_SELECTED = "#FF0000";
	private int screenWidth;
	private int screenHeight;
	private int buttonWidth;
	private int buttonHeight;
	private Bitmap bg;
	private Bitmap newGameBmp;
	private Bitmap optionsBmp;
	private Point selection1 = new Point(0, 0);
	private Point selection2 = new Point(0, 0);
	private List<Point> path = null;
	private Line nextPair = null;
	private long startTime;
	private long playTime;
	private long baseTime;
	private Timer timer;
	private Tileset tileset;
	private Handler timerHandler = new hHandler(this);
	private boolean timerRegistered = false;
	private ShisenSho app;
	private StatePlay cstate;
	private StatePaint pstate;
	private Canvas canvas = null;
	private SurfaceHolder surfaceHolder = null;
	private String time = INVALID_TIME;
	private GestureDetectorCompat mDetector;

	public ShisenShoView(ShisenSho shisenSho) {
		super(shisenSho);
		this.app = shisenSho;
		cstate = StatePlay.UNINITIALIZED;
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
		tileset = new Tileset(shisenSho);
		mDetector = new GestureDetectorCompat(getContext(), new MyGestureListener());
	}

	public ShisenShoView(Context ctx) {
		super(ctx);
		this.app = (ShisenSho) ctx;
		cstate = StatePlay.UNINITIALIZED;
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
		tileset = new Tileset((ShisenSho) ctx);
		mDetector = new GestureDetectorCompat(getContext(), new MyGestureListener());
	}

	public void pauseTime() {
		updateTime();
		baseTime = playTime;
		startTime = System.currentTimeMillis();

	}

	private void updateTime() {
		if (cstate != StatePlay.RESTARTING) {
			playTime = (System.currentTimeMillis() - startTime) / 1000 + baseTime;
		}
	}

	public void resumeTime() {
		startTime = System.currentTimeMillis();
		updateTime();
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.hint:
				this.postDelayed(new Runnable() {
					public void run() {
						onHintActivate();
					}
				}, 100);
				return true;
			case R.id.undo:
				this.postDelayed(new Runnable() {
					public void run() {
						onUndoActivate();
					}
				}, 100);
				return true;
			case R.id.clean:
				this.postDelayed(new Runnable() {
					public void run() {
						reset();//FIXME TODO
					}
				}, 100);
				return true;
			case R.id.options:
				return true;
			case R.id.about:
				return true;
			default:
				return false;
		}
	}

	public void reset() {
		control(StatePlay.UNINITIALIZED);
		paint(StatePaint.STARTING);
	}

	private void onHintActivate() {
		if ((cstate != StatePlay.RESTARTING) && (cstate != StatePlay.STARTING)) {
			nextPair = app.board.getNextPair();
			paint(StatePaint.HINT);
			app.sleep(10);
			paint(StatePaint.BOARD);
			control(StatePlay.IDLE);
		}
	}

	private void onUndoActivate() {
		if (app.board.getCanUndo()) {
			if (cstate == StatePlay.RESTARTING && !timerRegistered) {
				// Reprogram the time update that had been
				// deactivated with the game over status
				registerTimer();
			}
			app.board.undo();
			paint(StatePaint.BOARD);
			//undo.sensitive=app.board.getCanUndo();
			control(StatePlay.IDLE);
		}
	}

	private void onUpdateTime() {
		paint(pstate);
		if (cstate == StatePlay.RESTARTING) {
			unregisterTimer();
		}
	}

	private void paint(StatePaint pstate) {
		this.pstate = pstate;
		repaint();
	}

	private void unregisterTimer() {
		if (timer == null)
			return; // Already unregistered
		timer.cancel();
		timer = null;
		timerRegistered = false;
	}

	public synchronized void repaint() {
		if (surfaceHolder == null) return;
		try {
			if (canvas == null) canvas = surfaceHolder.lockCanvas(null);
			if (canvas == null) return;
			if (cstate == StatePlay.UNINITIALIZED) initializeGame();
			doDraw(canvas);
		} finally {
			if (canvas != null) {
				surfaceHolder.unlockCanvasAndPost(canvas);
				canvas = null;
			}
		}
	}

	private void initializeGame() {
		loadBackground();
		screenWidth = getWidth();
		screenHeight = getHeight();
		loadButtons();
		loadTileset();
		pstate = StatePaint.STARTING;
		control(StatePlay.STARTING);
	}

	private void startNewGame() {
		app.newPlay();
		startTime = System.currentTimeMillis();
		playTime = 0;
		baseTime = 0;
		if (!timerRegistered) {
			registerTimer();
		}
		nextPair = app.board.getNextPair();
		control(StatePlay.IDLE);
		paint(StatePaint.BOARD);
	}

	protected void doDraw(Canvas canvas) {
		try {
			if (canvas == null) return;

			// Board upper left corner on screen
			int x0 = 0;
			int y0 = 0;

			if (app != null && app.board != null) {
				x0 = (screenWidth - app.board.boardSizeX * tileset.tileWidth) / 2;
				y0 = (screenHeight - app.board.boardSizeY * tileset.tileHeight) / 2;
			}

			int selectcolor = Color.parseColor(COLOR_SELECTED);
			int hintcolor = Color.parseColor(COLOR_HINT);

			// Background painting
			int bgWidth = bg.getWidth();
			int bgHeight = bg.getHeight();
			for (int i = 0; i < screenHeight / bgHeight + 1; i++) {
				for (int j = 0; j < screenWidth / bgWidth + 1; j++) {
					canvas.drawBitmap(bg, j * bgWidth, i * bgHeight, null);
				}
			}

			// Board painting
			// Max visible size: 7x17
			if (app != null && app.board != null) {
				for (int i = 0; i < app.board.boardSizeY; i++) {
					for (int j = 0; j < app.board.boardSizeX; j++) {
						char piece = app.board.board[i][j];
						if (piece != 0) {
							canvas.drawBitmap(
									tileset.tile[piece],
									x0 + j * tileset.tileWidth,
									y0 + i * tileset.tileHeight,
									null);
						}
					}
				}
			}

			// rectangle for selection 1
			switch (pstate) {
				case SELECTED1:
				case SELECTED2:
				case MATCHED:
					highlightTile(canvas, x0, y0, selection1, selectcolor);
					break;
				default:
					break;
			}

			// rectangle for selection 2
			switch (pstate) {
				case SELECTED2:
				case MATCHED:
					highlightTile(canvas, x0, y0, selection2, selectcolor);
					break;
				default:
					break;
			}

			// Matching path
			switch (pstate) {
				case MATCHED:
					if (path != null) {
						Point p0 = null;
						for (Point p1 : path) {
							if (p0 != null) {
								drawLine(canvas, x0, y0, p0, p1, selectcolor);
							}
							p0 = p1;
						}
					}
					break;
				default:
					break;
			}

			// hint rectangles
			switch (pstate) {
				case HINT:
					if (nextPair != null) {
						Line pair = nextPair;
						Point a = pair.a;
						Point b = pair.b;
						path = app.board.getPath(a, b);

						highlightTile(canvas, x0, y0, a, hintcolor);

						if (path != null) {
							Point p0 = null;
							for (Point p1 : path) {
								if (p0 != null) {
									drawLine(canvas, x0, y0, p0, p1, hintcolor);
								}
								p0 = p1;
							}
							path = null;
						}

						highlightTile(canvas, x0, y0, b, hintcolor);
					}
					break;
				default:
					break;
			}

			// stuff with text
			switch (pstate) {
				case STARTING:
				case WIN:
				case LOSE:
					String msg;
					switch (pstate) {
						case WIN:
							msg = "You Win!";
							break;
						case LOSE:
							msg = "Game Over";
							break;
						default:
							msg = "FreeShisen";
							break;
					}
					drawMessage(canvas, screenWidth / 2, (screenHeight / 2), true,
							msg, 100);
					drawButtons(canvas, screenWidth / 2, (screenHeight / 3) * 2);
					break;
				default:
					break;
			}

			// time
			switch (pstate) {
				case BOARD:
				case SELECTED1:
				case SELECTED2:
				case MATCHED:
				case WIN:
				case LOSE:
				case HINT:
				case TIME:
					updateTime();
					int hours = (int) (playTime / (60 * 60));
					int minutes = (int) ((playTime / 60) % 60);
					int seconds = (int) (playTime % 60);
					if (hours < 10) {
						time = String.format(Locale.US, "%01d:%02d:%02d",
								hours, minutes, seconds);
					} else {
						time = INVALID_TIME;
					}

					int timePosX = screenWidth - 120;
					int timePosY = screenHeight - 10;

					if (app.timeCounter) {
						drawMessage(canvas, timePosX, timePosY, false, time, 30);
					}
					break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void loadBackground() {
		BitmapFactory.Options ops = new BitmapFactory.Options();
		ops.inScaled = false;
		bg = BitmapFactory.decodeResource(getResources(), R.drawable.kshisen_bgnd, ops);
		bg.setDensity(Bitmap.DENSITY_NONE);
	}

	private void loadButtons() {
		BitmapFactory.Options ops = new BitmapFactory.Options();
		ops.inScaled = false;
		Bitmap buttons = BitmapFactory.decodeResource(app.getResources(), R.drawable.gamebuttons, ops);
		buttons.setDensity(Bitmap.DENSITY_NONE);
		// FIXME hardcoded: buttons are 4 normal tiles wide
		// FIXME: refactor into a static Tileset helper method or whatever
		float scalex = ((float) (screenWidth - 2) / 17) / (buttons.getWidth() / 8);
		float scaley = ((float) (screenHeight - 2) / 7) / buttons.getHeight();
		if (scaley < scalex) {
			scalex = scaley;
		} else {
			scaley = scalex;
		}
		Matrix matrix = new Matrix();
		matrix.setScale(scalex, scaley);
		newGameBmp = Bitmap.createBitmap(buttons, 0, 0,
				buttons.getWidth() / 2, buttons.getHeight(), matrix, false);
		optionsBmp = Bitmap.createBitmap(buttons, buttons.getWidth() / 2, 0,
				buttons.getWidth() / 2, buttons.getHeight(), matrix, false);

		buttonWidth = newGameBmp.getWidth();
		buttonHeight = newGameBmp.getHeight();
	}

	public void loadTileset() {
		tileset.loadTileset(screenWidth, screenHeight);
	}

	private void control(StatePlay cstate) {
		Log.d("DEBUGS", "state:"+cstate);
		this.cstate = cstate;
	}

	private void registerTimer() {
		if (timer != null)
			return; // Already registered
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				timerHandler.sendEmptyMessage(Activity.RESULT_OK);
			}
		}, 0, 1000);
		timerRegistered = true;
	}

	private void highlightTile(Canvas canvas, int x0, int y0, Point p, int color) {
		Paint paint = new Paint();
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(color);
		paint.setStyle(Style.STROKE);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);
		paint.setStrokeWidth(3);
		Rect r = new Rect(
				x0 + p.j * tileset.tileWidth - 2,
				y0 + p.i * tileset.tileHeight - 2,
				x0 + p.j * tileset.tileWidth + tileset.tileWidth + 2,
				y0 + p.i * tileset.tileHeight + tileset.tileHeight + 2);
		canvas.drawRect(r, paint);
	}

	private void drawLine(Canvas canvas, int x0, int y0, Point p0, Point p1, int color) {
		Paint paint = new Paint();
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(color);
		paint.setStyle(Style.STROKE);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);
		paint.setStrokeWidth(3);
		canvas.drawLine(
				x0 + p0.j * tileset.tileWidth - 2 + (tileset.tileWidth / 2),
				y0 + p0.i * tileset.tileHeight - 2 + (tileset.tileHeight / 2),
				x0 + p1.j * tileset.tileWidth - 2 + (tileset.tileWidth / 2),
				y0 + p1.i * tileset.tileHeight - 2 + (tileset.tileHeight / 2), paint);
	}

	public static void drawMessage(Canvas canvas, int x, int y,
								   boolean centered, String message, float textSize) {
		Paint paint = new Paint();
		paint.setLinearText(true);
		paint.setAntiAlias(true);
		paint.setTextAlign(centered ? Align.CENTER : Align.LEFT);
		paint.setTypeface(Typeface.SANS_SERIF);
		paint.setFakeBoldText(true);
		paint.setTextSize(textSize);
		paint.setColor(Color.parseColor(COLOR_TEXT_SHADOW));
		canvas.drawText(message, x + 1, y + 1, paint);
		paint.setColor(Color.parseColor(COLOR_TEXT));
		canvas.drawText(message, x, y, paint);
	}

	private void drawButtons(Canvas canvas, int x, int y) {
		int bw = buttonWidth;
		canvas.drawBitmap(newGameBmp, x - (bw + bw / 8), y, null);
		canvas.drawBitmap(optionsBmp, x + (bw / 8), y, null);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		this.mDetector.onTouchEvent(event);
		return true;
	}

	private void doPlaySoundEffect() {
		super.playSoundEffect(android.view.SoundEffectConstants.CLICK);
		//	Log.d("ShisenSho", "SOUND EFFECT!!! BADABUMM!");
	}

	private void onClick(int x, int y) {
		Log.d("DEBUGS", "onclick:"+cstate);
		try {
			if ((cstate != StatePlay.STARTING) && (cstate != StatePlay.RESTARTING))
			{
				int i = (y - (screenHeight - app.board.boardSizeY * tileset.tileHeight) / 2) / tileset.tileHeight;
				int j = (x - (screenWidth - app.board.boardSizeX * tileset.tileWidth) / 2) / tileset.tileWidth;

				switch (cstate) {
					case IDLE:
						if (i >= 0 && i < app.board.boardSizeY && j >= 0
								&& j < app.board.boardSizeX
								&& app.board.board[i][j] != 0) {
							selection1.set(i, j);
							paint(StatePaint.SELECTED1);
							control(StatePlay.SELECTED1);
							doPlaySoundEffect();
						}
						break;
					case SELECTED1:
						if (i >= 0 && i < app.board.boardSizeY && j >= 0
								&& j < app.board.boardSizeX
								&& app.board.board[i][j] != 0) {
							if (selection1.equals(i, j)) {
								paint(StatePaint.BOARD);
								control(StatePlay.IDLE);
							} else {
								selection2.set(i, j);
								paint(StatePaint.SELECTED2);

								Point a = selection1.copy();
								Point b = selection2.copy();
								path = app.board.getPath(a, b);
								paint(StatePaint.MATCHED);
								app.sleep(2);
								paint(StatePaint.BOARD);
								if (path.size() > 0) {
									app.board.play(a, b);
								}
								path = null;
								paint(StatePaint.BOARD);

								nextPair = app.board.getNextPair();
								if (nextPair == null) {
									if (app.board.getNumPieces() == 0) {
										paint(StatePaint.WIN);
										checkforhiscore();
									} else {
										paint(StatePaint.LOSE);
									}
									control(StatePlay.RESTARTING);
								} else {
									control(StatePlay.IDLE);
								}
							}
							doPlaySoundEffect();
						}
						break;
					default:
						break;
				}
			} else {
				// STARTING or RESTARTING:
				int bw = buttonWidth;
				int bh = buttonHeight;
				int midx = screenWidth / 2;
				int midy = (screenHeight / 3) * 2;
				if (((midx - (bw + bw / 8)) < x) && (x < (midx - (bw / 8)))
						&& (midy < y) && (y < midy + bh)) {
					// "new game"
					startNewGame();
					doPlaySoundEffect();
					Log.d("DEBUGS", "new");
				} else if (((midx + (bw / 8)) < x) && (x < (midx + (bw + bw / 8)))
						&& (midy < y) && (y < midy + bh)) {
					// "options"
					app.activity.openOptionsMenu();
					doPlaySoundEffect();
					Log.d("DEBUGS", "options");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void checkforhiscore() {
		if (timerRegistered) {
			unregisterTimer();
		}
		final String[] sizes = {"S", "M", "L"};
		final String[] diffs = {"E", "H"};
		String prefname1 = "hiscore_" + diffs[app.difficulty - 1] + sizes[app.size - 1] + "1";
		String prefname2 = "hiscore_" + diffs[app.difficulty - 1] + sizes[app.size - 1] + "2";
		// get hiscores for current size/difficulty
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(app);
		String besttime1 = sp.getString(prefname1, INVALID_TIME);
		String besttime2 = sp.getString(prefname2, INVALID_TIME);
		// did we win something?
		if (time.compareTo(besttime2) < 0) {
			// score!
			new AlertDialog.Builder(app.activity)
					.setTitle(R.string.hiscore_title)
					.setCancelable(true)
					.setIcon(R.drawable.icon)
					.setPositiveButton(android.R.string.ok, null)
					.setMessage(R.string.hiscore_text)
					.create()
					.show();

			SharedPreferences.Editor editor = sp.edit();
			if (time.compareTo(besttime1) < 0) {
				editor.putString(prefname1, time);
				editor.putString(prefname2, besttime1);
			} else {
				editor.putString(prefname2, time);
			}
			editor.commit();
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		surfaceHolder = holder;
		repaint();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
							   int height) {
		surfaceHolder = holder;
		if (cstate != StatePlay.RESTARTING && !timerRegistered) {
			registerTimer();
		}
		repaint();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		surfaceHolder = null;
		if (timerRegistered) {
			unregisterTimer();
		}
	}

	public void onOptionsChanged() {
		this.postDelayed(new Runnable() {
			public void run() {
				onOptionsChangedActivate();
			}
		}, 100);
	}

	public void onOptionsChangedActivate() {
		new AlertDialog.Builder(app.activity)
				.setTitle(R.string.prefchange_confirm_title)
				.setCancelable(true)
				.setIcon(R.drawable.icon)
				.setPositiveButton(android.R.string.ok, null)
				.setMessage(R.string.prefchange_confirm_text)
				.create()
				.show();
	}

	private enum StatePlay {UNINITIALIZED, STARTING, IDLE, SELECTED1, RESTARTING}

	private enum StatePaint {STARTING, BOARD, SELECTED1, SELECTED2, MATCHED, WIN, LOSE, HINT, TIME}

	private static class hHandler extends Handler {
		private final WeakReference<ShisenShoView> mTarget;

		hHandler(ShisenShoView target) {
			mTarget = new WeakReference<>(target);
		}

		@Override
		public void handleMessage(Message msg) {
			ShisenShoView target = mTarget.get();
			if (target != null)
				target.onUpdateTime();
		}
	}

	private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

		final float scale = getResources().getDisplayMetrics().density;

		@Override
		public boolean onSingleTapUp(MotionEvent event) {
			//Log.d("DEBUGS", "onSTUp");
			onClick(Math.round(event.getX()), Math.round(event.getY()));
			return true;
		}

		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2,
							   float dX, float dY) {
			//Log.d("DEBUGS", "onFling: 1:" + event1.getX() + "," + event1.getY() + " 2:" + event2.getX() + "," + event2.getY());
			//Log.d("DEBUGS", "onFling: scale:" + (30.0 * scale));
			if (abs(event1.getY() - event2.getY()) > (30.0 * scale)) {
				app.activity.openOptionsMenu();
			}

			return true;
		}

		@Override
		public boolean onDown(MotionEvent event) {
			//Log.d("DEBUGS", "onDowni2");
			return true;
		}
	}
}
