package de.mrmatt.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import de.mrmatt.game.Engine;

/**
 * Das Spielfeld (31 x 18 Kacheln). Zeigt ein Brett an, spielt die Ereignisse einer Physik-Runde
 * als Animation ab und liefert Wisch- und Tipp-Eingaben.
 */
public class BoardView extends View {

    public interface Listener {
        /** Wisch-/Ziehgeste: eine Richtung (Engine.LEFT ... DOWN). */
        void onSwipe(int dir);

        /** Tipp auf ein Feld (Spalte, Zeile). */
        void onTap(int col, int row);
    }

    private static final int FRAME_MS = 28;

    private final byte[] disp = new byte[Engine.N];
    private int mx, my;
    private boolean dead;

    private Tiles tiles;
    private int tile, ox, oy;
    private final Paint bgPaint = new Paint();
    private final Paint fieldPaint = new Paint();
    private final Rect dst = new Rect();
    private final Handler handler = new Handler();

    // Animation
    private int[] events;
    private int evCount, evPos, perFrame;
    private byte[] finalCells;
    private int finalMx, finalMy;
    private boolean finalDead;
    private Runnable done;
    private boolean animating;

    // Touch
    private Listener listener;
    private float anchorX, anchorY;
    private boolean swiped;

    public BoardView(Context ctx) {
        super(ctx);
        bgPaint.setColor(Ui.BG);
        fieldPaint.setColor(0xFF111A2E);
        setFocusable(false);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    /** Zeigt ein Brett direkt an (beendet eine laufende Animation ohne Rueckruf). */
    public void show(byte[] cells, int x, int y, boolean isDead) {
        stopAnimation();
        System.arraycopy(cells, 0, disp, 0, Engine.N);
        mx = x;
        my = y;
        dead = isDead;
        invalidate();
    }

    /**
     * Spielt die Ereignisse einer Physik-Runde ab: Start ist das Brett "stage", am Ende wird
     * finalCells angezeigt und danach done aufgerufen.
     */
    public void play(byte[] stage, int[] ev, int count, byte[] fin, int x, int y, boolean isDead, Runnable whenDone) {
        stopAnimation();
        System.arraycopy(stage, 0, disp, 0, Engine.N);
        events = new int[count];
        System.arraycopy(ev, 0, events, 0, count);
        evCount = count;
        evPos = 0;
        int n = count / Engine.EV_SIZE;
        perFrame = Math.max(1, n / 45);
        finalCells = new byte[Engine.N];
        System.arraycopy(fin, 0, finalCells, 0, Engine.N);
        finalMx = x;
        finalMy = y;
        finalDead = isDead;
        mx = x;
        my = y;
        dead = false;
        done = whenDone;
        animating = true;
        invalidate();
        handler.postDelayed(frame, FRAME_MS);
    }

    /** Springt zum Endzustand der laufenden Animation (ohne Rueckruf). */
    public void finishNow() {
        if (!animating) return;
        stopAnimation();
        System.arraycopy(finalCells, 0, disp, 0, Engine.N);
        mx = finalMx;
        my = finalMy;
        dead = finalDead;
        invalidate();
    }

    private void stopAnimation() {
        animating = false;
        handler.removeCallbacks(frame);
        done = null;
    }

    private final Runnable frame = new Runnable() {
        public void run() {
            if (!animating) return;
            for (int k = 0; k < perFrame && evPos < evCount; k++) {
                Engine.applyEvent(disp, events, evPos);
                evPos += Engine.EV_SIZE;
            }
            if (evPos >= evCount) {
                System.arraycopy(finalCells, 0, disp, 0, Engine.N);
                dead = finalDead;
                animating = false;
                invalidate();
                Runnable d = done;
                done = null;
                if (d != null) d.run();
            } else {
                invalidate();
                handler.postDelayed(this, FRAME_MS);
            }
        }
    };

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int t = Math.max(4, Math.min(w / Engine.COLS, h / Engine.ROWS));
        tile = t;
        ox = (w - t * Engine.COLS) / 2;
        oy = (h - t * Engine.ROWS) / 2;
        if (tiles == null || tiles.size != t) tiles = Tiles.create(t);
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
        if (tiles == null) return;
        c.drawRect(ox, oy, ox + tile * Engine.COLS, oy + tile * Engine.ROWS, fieldPaint);
        for (int r = 0; r < Engine.ROWS; r++) {
            for (int col = 0; col < Engine.COLS; col++) {
                byte t = disp[r * Engine.COLS + col];
                if (t == Engine.EMPTY) continue;
                Bitmap b = tiles.forType(t);
                if (b != null) c.drawBitmap(b, ox + col * tile, oy + r * tile, null);
            }
        }
        c.drawBitmap(dead ? tiles.mattDead : tiles.matt, ox + mx * tile, oy + my * tile, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (listener == null) return false;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                anchorX = e.getX();
                anchorY = e.getY();
                swiped = false;
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - anchorX, dy = e.getY() - anchorY;
                float thr = Math.max(tile * 0.9f, 12f);
                if (Math.max(Math.abs(dx), Math.abs(dy)) >= thr) {
                    int dir;
                    if (Math.abs(dx) > Math.abs(dy)) dir = dx < 0 ? Engine.LEFT : Engine.RIGHT;
                    else dir = dy < 0 ? Engine.UP : Engine.DOWN;
                    anchorX = e.getX();
                    anchorY = e.getY();
                    swiped = true;
                    listener.onSwipe(dir);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (!swiped && tile > 0) {
                    int col = (int) ((e.getX() - ox) / tile);
                    int row = (int) ((e.getY() - oy) / tile);
                    if (col >= 0 && col < Engine.COLS && row >= 0 && row < Engine.ROWS) listener.onTap(col, row);
                }
                return true;
            default:
                return true;
        }
    }
}
