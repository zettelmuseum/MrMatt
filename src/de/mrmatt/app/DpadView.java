package de.mrmatt.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import de.mrmatt.game.Engine;

/** Halbtransparentes Steuerkreuz mit Dauerfeuer beim Halten. */
public class DpadView extends View {

    public interface Listener {
        void onDir(int dir);
    }

    private static final int REPEAT_MS = 150;

    private Listener listener;
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowOn = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Handler handler = new Handler();
    private int current = -1;

    public DpadView(Context ctx) {
        super(ctx);
        bg.setColor(0x55000000);
        arrow.setColor(0xB0FFFFFF);
        arrowOn.setColor(0xFFFFD54F);
        setFocusable(false);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f, r = Math.min(w, h) / 2f;
        c.drawCircle(cx, cy, r, bg);
        float a = r * 0.62f, b = r * 0.2f, s = r * 0.22f;
        triangle(c, cx, cy - a, cx - s, cy - b, cx + s, cy - b, current == Engine.UP);
        triangle(c, cx, cy + a, cx - s, cy + b, cx + s, cy + b, current == Engine.DOWN);
        triangle(c, cx - a, cy, cx - b, cy - s, cx - b, cy + s, current == Engine.LEFT);
        triangle(c, cx + a, cy, cx + b, cy - s, cx + b, cy + s, current == Engine.RIGHT);
    }

    private void triangle(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3, boolean on) {
        path.reset();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();
        c.drawPath(path, on ? arrowOn : arrow);
    }

    private int dirAt(float x, float y) {
        float dx = x - getWidth() / 2f, dy = y - getHeight() / 2f;
        float dead = Math.min(getWidth(), getHeight()) * 0.08f;
        if (Math.abs(dx) < dead && Math.abs(dy) < dead) return -1;
        if (Math.abs(dx) > Math.abs(dy)) return dx < 0 ? Engine.LEFT : Engine.RIGHT;
        return dy < 0 ? Engine.UP : Engine.DOWN;
    }

    private final Runnable repeat = new Runnable() {
        public void run() {
            if (current >= 0 && listener != null) {
                listener.onDir(current);
                handler.postDelayed(this, REPEAT_MS);
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                int d = dirAt(e.getX(), e.getY());
                if (d != current) {
                    current = d;
                    handler.removeCallbacks(repeat);
                    if (d >= 0 && listener != null) {
                        listener.onDir(d);
                        handler.postDelayed(repeat, REPEAT_MS);
                    }
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                current = -1;
                handler.removeCallbacks(repeat);
                invalidate();
                return true;
            default:
                return true;
        }
    }
}
