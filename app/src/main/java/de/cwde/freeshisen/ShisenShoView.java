package de.cwde.freeshisen;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
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

	private enum StatePlay { UNINITIALIZED, IDLE, SELECTED1, GAMEOVER }
	private enum StatePaint { BOARD, SELECTED1, SELECTED2, MATCHED, WIN, LOSE, HINT, TIME }

	private int screenWidth;
	private int screenHeight;
	private Bitmap bg;
	private Point selection1 = new Point(0, 0);
	private Point selection2 = new Point(0, 0);
	private List<Point> path = null;
	private List<Line> pairs = null;
	private long startTime;
	private long playTime;
	private long baseTime;
	private Timer timer;
	private Tileset tileset;

	static class hHandler extends Handler {
		private final WeakReference<ShisenShoView> mTarget;

		hHandler(ShisenShoView target) {
			mTarget = new WeakReference<ShisenShoView>(target);
		}

		@Override
		public void handleMessage(Message msg) {
			ShisenShoView target = mTarget.get();
			if (target != null)
				target.onUpdateTime();
		}
	}

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

	private void paint(StatePaint pstate) {
		this.pstate=pstate;
		repaint();
	}

	private void control(StatePlay cstate) {
		this.cstate=cstate;
	}

	private void loadBackground() {
		BitmapFactory.Options ops = new BitmapFactory.Options();
		ops.inScaled = false;
		bg = BitmapFactory.decodeResource(getResources(), R.drawable.kshisen_bgnd, ops);
		bg.setDensity(Bitmap.DENSITY_NONE);
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

	private void unregisterTimer() {
		if (timer == null)
			return; // Already unregistered
		timer.cancel();
		timer = null;
		timerRegistered = false;
	}

	public void pauseTime() {
		updateTime();
		baseTime = playTime;
		startTime = System.currentTimeMillis();

	}

	public void resumeTime() {
		startTime = System.currentTimeMillis();
		updateTime();
	}

	private void updateTime() {
		if (cstate!=StatePlay.GAMEOVER) {
			playTime = (System.currentTimeMillis()-startTime)/1000+baseTime;
		}
	}

	public void loadTileset() {
		tileset.loadTileset(screenWidth, screenHeight);
	}

	private void initializeGame() {
		loadBackground();
		screenWidth=getWidth();
		screenHeight=getHeight();
		loadTileset();
		//undo.sensitive=false;
		pstate=StatePaint.BOARD;
		app.newPlay();
		control(StatePlay.IDLE);
		startTime=System.currentTimeMillis();
		playTime=0;
		baseTime=0;
		if (!timerRegistered) {
			registerTimer();
		}
		pairs=app.board.getPairs(1);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.hint:
			this.postDelayed(new Runnable() { public void run() { onHintActivate(); } }, 100);
			return true;
		case R.id.undo:
			this.postDelayed(new Runnable() { public void run() { onUndoActivate(); } }, 100);
			return true;
		case R.id.clean:
			this.postDelayed(new Runnable() { public void run() { reset(); } }, 100);
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
		paint(StatePaint.BOARD);
	}

	private void onHintActivate() {
		if (cstate!=StatePlay.GAMEOVER) {
			pairs=app.board.getPairs(1);
			paint(StatePaint.HINT);
			app.sleep(10);
			paint(StatePaint.BOARD);
			control(StatePlay.IDLE);
		}
	}

	private void onUndoActivate() {
		if (app.board.getCanUndo()) {
			if (cstate==StatePlay.GAMEOVER && !timerRegistered) {
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
		if (cstate==StatePlay.GAMEOVER) {
			unregisterTimer();
		}
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

	protected void doDraw(Canvas canvas) {
		try {
			if (canvas == null) return;

			// Board upper left corner on screen
			int x0=0;
			int y0=0;

			if (app!=null && app.board!=null) {
				x0=(screenWidth-app.board.boardSize[1]*tileset.tileWidth)/2;
				y0=(screenHeight-app.board.boardSize[0]*tileset.tileHeight)/2;
			}

			int selectcolor = Color.parseColor(COLOR_SELECTED);
			int hintcolor = Color.parseColor(COLOR_HINT);

			// Background & board painting
			switch (pstate) {
			case BOARD:
			case SELECTED1:
			case SELECTED2:
			case MATCHED:
			case WIN:
			case LOSE:
			case HINT:
			case TIME:
				// Background painting
				int bgWidth = bg.getWidth();
				int bgHeight = bg.getHeight();
				for (int i=0; i<screenHeight/bgHeight+1; i++) {
					for (int j=0; j<screenWidth/bgWidth+1; j++) {
						canvas.drawBitmap(bg, j*bgWidth, i*bgHeight, null);
					}
				}

				// Board painting
				// Max visible size: 7x17
				if (app!=null && app.board!=null) {
					for (int i=0;i<app.board.boardSize[0];i++) {
						for (int j=0;j<app.board.boardSize[1];j++) {
							// Tiles are 56px height, 40px width each
							char piece=app.board.board[i][j];
							if (piece!=0) {
								canvas.drawBitmap(
										tileset.tile[piece],
										x0+j*tileset.tileWidth,
										y0+i*tileset.tileHeight,
										null);
							}
						}
					}
				}
				break;
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
				if (path!=null) {
					Point p0=null;
					for (Point p1 : path) {
						if (p0!=null) {
							drawLine(canvas, x0, y0, p0, p1, selectcolor);
						}
						p0=p1;
					}
				}
				break;
			default:
				break;
			}

			// hint rectangles
			switch (pstate) {
			case HINT:
				if (pairs != null && pairs.size() > 0) {
					Line pair = pairs.get(0);
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

			// Win & loose notifications
			switch (pstate) {
			case WIN:
				drawMessage(canvas, screenWidth / 2, screenHeight / 2, true,
						"You Win!", 100);
				break;
			case LOSE:
				drawMessage(canvas, screenWidth / 2, screenHeight / 2, true,
						"Game Over", 100);
				break;
			default:
				break;
			}

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

				int timePosX=screenWidth-120;
				int timePosY=screenHeight-10;

				if (app.timeCounter) {
					drawMessage(canvas, timePosX, timePosY, false, time, 30);
				}
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

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
		try {
			int i=(y-(screenHeight-app.board.boardSize[0]*tileset.tileHeight)/2)/tileset.tileHeight;
			int j=(x-(screenWidth-app.board.boardSize[1]*tileset.tileWidth)/2)/tileset.tileWidth;

			switch (cstate) {
			case IDLE:
				if (i >= 0 && i < app.board.boardSize[0] && j >= 0
						&& j < app.board.boardSize[1]
						&& app.board.board[i][j] != 0) {
					selection1.set(i, j);
					paint(StatePaint.SELECTED1);
					control(StatePlay.SELECTED1);
					doPlaySoundEffect();
				}
				break;
			case SELECTED1:
				if (i >= 0 && i < app.board.boardSize[0] && j >= 0
				&& j < app.board.boardSize[1]
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

						pairs = app.board.getPairs(1);
						if (pairs.size() == 0) {
							if (app.board.getNumPieces() == 0) {
								paint(StatePaint.WIN);
								checkforhiscore();
							} else {
								paint(StatePaint.LOSE);
							}
							control(StatePlay.GAMEOVER);
						} else {
							control(StatePlay.IDLE);
						}
						//undo.sensitive=app.board.getCanUndo();
					}
					doPlaySoundEffect();
				}
				break;
			case GAMEOVER:
				reset();
				paint(StatePaint.BOARD);
				doPlaySoundEffect();
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void checkforhiscore() {
		if (timerRegistered) {
			unregisterTimer();
		}
		final String[] sizes = { "S", "M", "L" };
		final String[] diffs = { "E", "H" };
		String prefname1 = "hiscore_" + diffs[app.difficulty-1] + sizes[app.size-1] + "1";
		String prefname2 = "hiscore_" + diffs[app.difficulty-1] + sizes[app.size-1] + "2";
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

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		surfaceHolder = holder;
		if (cstate!=StatePlay.GAMEOVER && !timerRegistered) {
			registerTimer();
		}
		repaint();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		surfaceHolder = holder;
		repaint();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		surfaceHolder = null;
		if (timerRegistered) {
			unregisterTimer();
		}
	}

	public void onOptionsChanged()
	{
		this.postDelayed(new Runnable() { public void run() { onOptionsChangedActivate(); } }, 100);
	}

	public void onOptionsChangedActivate()
	{
		new AlertDialog.Builder(app.activity)
		.setTitle(R.string.prefchange_confirm_title)
		.setCancelable(true)
		.setIcon(R.drawable.icon)
		.setPositiveButton(android.R.string.ok, null)
		.setMessage(R.string.prefchange_confirm_text)
		.create()
		.show();
	}

	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

		final float scale = getResources().getDisplayMetrics().density;

		@Override
		public boolean onDown(MotionEvent event) {
			Log.d("DEBUGS", "onDowni2");
			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent event)
		{
			Log.d("DEBUGS", "onSTUp");
			onClick(Math.round(event.getX()),Math.round(event.getY()));
			return true;
		}

		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2,
							   float dX, float dY) {
			Log.d("DEBUGS", "onFling: 1:" + event1.getX() + "," + event1.getY() + " 2:"+ event2.getX()+ "," + event2.getY());
			Log.d("DEBUGS", "onFling: scale:" + (30.0*scale));
			// TODO: options menu handling
			if (abs(event1.getY() - event2.getY()) > (30.0*scale) )
			{
				app.activity.openOptionsMenu();
			}

			return true;
		}
	}
}
