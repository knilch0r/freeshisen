package de.cwde.freeshisen;

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
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
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class ShisenShoView extends SurfaceView implements SurfaceHolder.Callback {

	private static final String COLOR_TEXT = "#FFFFFF";
	private static final String COLOR_TEXT_SHADOW = "#000000";
	private static final String COLOR_HINT = "#F0C000";
	private static final String COLOR_SELECTED = "#FF0000";

	private enum StatePlay { UNINITIALIZED, IDLE, SELECTED1, SELECTED2, GAMEOVER };
	private enum StatePaint { BOARD, SELECTED1, SELECTED2, MATCHED, WIN, LOSE, HINT, TIME };

	private int screenWidth;
	private int screenHeight;
	private int tilesetRows;
	private int tilesetCols;
	private int tileHeight;
	private int tileWidth;
	private Bitmap bg;
	private Bitmap tile[];
	private Point selection1 = new Point(0,0);
	private Point selection2 = new Point(0,0);
	private List<Point> path=null;
	private List<Line> pairs=null;
	private long startTime;
	private long playTime;
	private long baseTime;
	private Timer timer;
	private static Handler timerHandler;

	private boolean timerRegistered=false;
	private ShisenSho app;
	private StatePlay cstate;
	private StatePaint pstate;
	private Canvas canvas = null;
	private SurfaceHolder surfaceHolder = null;

	public ShisenShoView(ShisenSho shishenSho) {
		super((Context) shishenSho);
		this.app = shishenSho;
		cstate = StatePlay.UNINITIALIZED;
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
	}

	public ShisenShoView(Context ctx) {
		super((Context) ctx);
		// silence lint?
	}

	private void paint(StatePaint pstate) {
		this.pstate=pstate;
		repaint();
	}

	private void control(StatePlay cstate) {
		this.cstate=cstate;
	}

	private void loadTileset() {
		BitmapFactory.Options ops = new BitmapFactory.Options();
		ops.inScaled = false;
		Bitmap tileset = BitmapFactory.decodeResource(getResources(), R.drawable.tileset, ops);
		tileset.setDensity(Bitmap.DENSITY_NONE);

		// The tile set has 4 rows x 9 columns
		tilesetRows = 4;
		tilesetCols = 9;
		int loadedtileWidth = tileset.getWidth()/tilesetCols;
		int loadedtileHeight = tileset.getHeight()/tilesetRows;
		tile = new Bitmap[tilesetRows*tilesetCols];

		// align to screen:
		// "large" is 16x6, and we want to have a nice border, so we use 17x7 and
		// choose the lowest scale so everything fits
		float scalex = ((float) screenWidth/17) / loadedtileWidth;
		float scaley = ((float) screenHeight/7) / loadedtileHeight;
		if (scaley < scalex) {
			scalex = scaley;
		} else {
			scaley = scalex;
		}
		Matrix matrix = new Matrix();
		matrix.setScale(scalex, scaley);

		int k=0;
		for (int i=0; i<tilesetRows; i++) {
			for (int j=0; j<tilesetCols; j++) {
				tile[k] = Bitmap.createBitmap(tileset, j*loadedtileWidth, i*loadedtileHeight,
						loadedtileWidth, loadedtileHeight, matrix, false);
				tile[k].setDensity(Bitmap.DENSITY_NONE);
				k++;
			}
		}
		tileWidth = tile[0].getWidth();
		tileHeight = tile[0].getHeight();
	}

	private void loadBackground() {
		BitmapFactory.Options ops = new BitmapFactory.Options();
		ops.inScaled = false;
		bg = BitmapFactory.decodeResource(getResources(), R.drawable.kshisen_bgnd, ops);
		bg.setDensity(Bitmap.DENSITY_NONE);
	}

	private void registerTimer() {
		if (timer!=null) return; // Already registered
		timerHandler = new Handler() {
			public void handleMessage(Message msg) {
				onUpdateTime();
			}
		};
		timer=new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				timerHandler.sendEmptyMessage(Activity.RESULT_OK);
			}
		}, 0, 1000);
		timerRegistered=true;
	}

	private void unregisterTimer() {
		if (timer==null) return; // Already unregistered
		timer.cancel();
		timer = null;
		timerHandler = null;
		timerRegistered=false;
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
		if (app.timeCounter && !timerRegistered) {
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
			if (cstate==StatePlay.GAMEOVER && app.timeCounter && !timerRegistered) {
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

	public void onTimeCounterActivate() {
		if (app.timeCounter && cstate!=StatePlay.GAMEOVER && !timerRegistered) {
			// Reprogram the time update that had been
			// deactivated with the time_counter=false
			registerTimer();
		}
	}

	private void onUpdateTime() {
		paint(pstate);
		if (!(app.timeCounter && cstate!=StatePlay.GAMEOVER)) {
			unregisterTimer();
		}
	}

	@SuppressWarnings("deprecation")
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

	public void repaint() {
		if (surfaceHolder == null) return;
		try {
			if (canvas == null) canvas = surfaceHolder.lockCanvas(null);
			if (canvas == null) return;
			if (cstate==StatePlay.UNINITIALIZED) initializeGame();
			synchronized (surfaceHolder) {
				doDraw(canvas);
			}
		} finally {
			if (canvas != null) {
				surfaceHolder.unlockCanvasAndPost(canvas);
				canvas = null;
			}
		}
	}

	protected void doDraw(Canvas canvas) {
		try {
			// Double buffering
			// Bitmap buffer = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
			//Canvas cbuffer = new Canvas(buffer);
			Canvas cbuffer = canvas;
			if (canvas == null) return;

			//super.onDraw(canvas);

			// Board upper left corner on screen
			int x0=0;
			int y0=0;

			if (app!=null && app.board!=null) {
				x0=(screenWidth-app.board.boardSize[1]*tileWidth)/2;
				y0=(screenHeight-app.board.boardSize[0]*tileHeight)/2;
			}

			int selectcolor = Color.parseColor(COLOR_SELECTED);
			int hintcolor = Color.parseColor(COLOR_HINT);
			Paint paint = new Paint();
			paint.setFlags(Paint.ANTI_ALIAS_FLAG);

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
						cbuffer.drawBitmap(bg, j*bgWidth, i*bgHeight, paint);
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
								cbuffer.drawBitmap(tile[piece], x0+j*tileWidth, y0+i*tileHeight, paint);
							}
						}
					}
				}
				break;
			}

			// Red rectangle for selection 1
			switch (pstate) {
			case SELECTED1:
			case SELECTED2:
			case MATCHED:
				highlightTile(cbuffer, x0, y0, selection1, selectcolor);
				break;
			}

			// Red rectangle for selection 2
			switch (pstate) {
			case SELECTED2:
			case MATCHED:
				highlightTile(cbuffer, x0, y0, selection2, selectcolor);
				break;
			}

			// Matching path
			switch (pstate) {
			case MATCHED:
				paint.setColor(selectcolor);
				paint.setStyle(Style.STROKE);
				paint.setStrokeCap(Cap.ROUND);
				paint.setStrokeJoin(Join.ROUND);
				paint.setStrokeWidth(3);

				if (path!=null) {
					Point p0=null;
					for (Point p1 : path) {
						if (p0!=null) {
							drawLine(cbuffer, x0, y0, p0, p1, paint);
						}
						p0=p1;
					}
				}
				break;
			}

			// Orange hint rectangles
			switch (pstate) {
			case HINT:
				if (pairs != null && pairs.size() > 0) {
					Line pair = pairs.get(0);
					Point a = pair.a;
					Point b = pair.b;
					path = app.board.getPath(a, b);
					paint.setColor(hintcolor);
					paint.setStyle(Style.STROKE);
					paint.setStrokeCap(Cap.ROUND);
					paint.setStrokeJoin(Join.ROUND);
					paint.setStrokeWidth(3);

					highlightTile(cbuffer, x0, y0, a, hintcolor);

					if (path != null) {
						Point p0 = null;
						for (Point p1 : path) {
							if (p0 != null) {
								drawLine(cbuffer, x0, y0, p0, p1, paint);
							}
							p0 = p1;
						}
						path = null;
					}

					highlightTile(cbuffer, x0, y0, b, hintcolor);
				}
				break;
			}

			// Win & loose notifications
			switch (pstate) {
			case WIN:
				drawMessage(cbuffer, screenWidth / 2, screenHeight / 2, true,
						"You Win!", 100);
				break;
			case LOSE:
				drawMessage(cbuffer, screenWidth / 2, screenHeight / 2, true,
						"Game Over", 100);
				break;
			}

			if (app.timeCounter) switch (pstate) {
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
				String time = String.format(Locale.US, "%01d:%02d:%02d",
						hours, minutes, seconds);

				int timePosX=screenWidth-120;
				int timePosY=screenHeight-10;

				drawMessage(cbuffer, timePosX, timePosY, false, time, 30);
				break;
			}

			// Debug messages
			/*
			debugMessage="StatePlay: "+cstate+"\n"+"StatePaint: "+pstate;
			if (debugMessage!=null && debugMessage.length()>0) {
				int l = 20;
				String lines[] = debugMessage.split("\n");
				for (int i=0; i<lines.length; i++) {
					drawMessage(cbuffer,1,l,false,lines[i],"#FFFF00",30);
					l+=30;
				}
			}
			 */

			// Double buffer dumping
			// canvas.drawBitmap(buffer, 0, 0, null);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void drawLine(Canvas cbuffer, int x0, int y0, Point p0, Point p1,
			Paint paint) {
		cbuffer.drawLine(
				x0 + p0.j * tileWidth - 2 + (tileWidth / 2),
				y0 + p0.i * tileHeight - 2 + (tileHeight / 2),
				x0 + p1.j * tileWidth - 2 + (tileWidth / 2),
				y0 + p1.i * tileHeight - 2 + (tileHeight / 2), paint);
	}

	private void highlightTile(Canvas cbuffer, int x0, int y0, Point p, int color) {
		Paint paint = new Paint();
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(color);
		paint.setStyle(Style.STROKE);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);
		paint.setStrokeWidth(3);
		Rect r = new Rect(
				x0 + p.j * tileWidth - 2,
				y0 + p.i * tileHeight - 2,
				x0 + p.j * tileWidth + tileWidth + 2,
				y0 + p.i * tileHeight + tileHeight + 2);
		cbuffer.drawRect(r, paint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction()==MotionEvent.ACTION_DOWN) {
			onClick(Math.round(event.getX()),Math.round(event.getY()));
		}
		return super.onTouchEvent(event);
	}

	private void onClick(int x, int y) {
		try {
			int i=(y-(screenHeight-app.board.boardSize[0]*tileHeight)/2)/tileHeight;
			int j=(x-(screenWidth-app.board.boardSize[1]*tileWidth)/2)/tileWidth;

			switch (cstate) {
			case IDLE:
				if (i >= 0 && i < app.board.boardSize[0] && j >= 0
				&& j < app.board.boardSize[1]
						&& app.board.board[i][j] != 0) {
					selection1.set(i, j);
					paint(StatePaint.SELECTED1);
					control(StatePlay.SELECTED1);
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
							} else {
								paint(StatePaint.LOSE);
							}
							control(StatePlay.GAMEOVER);
						} else {
							control(StatePlay.IDLE);
						}
						//undo.sensitive=app.board.getCanUndo();
					}
				}
				break;
			case GAMEOVER:
				reset();
				paint(StatePaint.BOARD);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		surfaceHolder = holder;
		if (cstate!=StatePlay.GAMEOVER && app.timeCounter && !timerRegistered) {
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
}
