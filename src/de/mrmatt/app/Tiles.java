package de.mrmatt.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import de.mrmatt.game.Engine;

import java.util.Random;

/** Zeichnet die Kacheln des Spielfelds (alles per Canvas, keine Bilddateien noetig). */
final class Tiles {
    final Bitmap earth, wall, stone, apple, box3, box2, box1, bomb, matt, mattDead;
    final int size;

    private Tiles(int t) {
        size = t;
        earth = makeEarth(t);
        wall = makeWall(t);
        stone = makeStone(t);
        apple = makeApple(t);
        box3 = makeBox(t, 3);
        box2 = makeBox(t, 2);
        box1 = makeBox(t, 1);
        bomb = makeBomb(t);
        matt = makeMatt(t, false);
        mattDead = makeMatt(t, true);
    }

    static Tiles create(int t) {
        return new Tiles(Math.max(4, t));
    }

    Bitmap forType(byte type) {
        switch (type) {
            case Engine.EARTH: return earth;
            case Engine.WALL: return wall;
            case Engine.STONE: return stone;
            case Engine.APPLE: return apple;
            case Engine.BOX: return box3;
            case Engine.BOX2: return box2;
            case Engine.BOX1: return box1;
            case Engine.BOMB: return bomb;
            default: return null;
        }
    }

    private static Paint fill(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        return p;
    }

    private static Paint stroke(int color, float w) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(color);
        p.setStrokeWidth(w);
        p.setStrokeCap(Paint.Cap.ROUND);
        return p;
    }

    private static Bitmap blank(int t) {
        return Bitmap.createBitmap(t, t, Bitmap.Config.ARGB_8888);
    }

    private static Bitmap makeEarth(int t) {
        Bitmap b = blank(t);
        Canvas c = new Canvas(b);
        c.drawRect(0, 0, t, t, fill(0xFF7A4B24));
        Random rnd = new Random(7);
        int dots = Math.max(6, t * t / 20);
        int[] cols = {0xFF5E3818, 0xFF96623A, 0xFF6B4020};
        float d = Math.max(1f, t / 12f);
        for (int i = 0; i < dots; i++) {
            float x = rnd.nextFloat() * (t - d);
            float y = rnd.nextFloat() * (t - d);
            c.drawRect(x, y, x + d, y + d, fill(cols[rnd.nextInt(cols.length)]));
        }
        c.drawRect(0, 0, t, Math.max(1f, t / 14f), fill(0xFF4F9A35));
        return b;
    }

    private static Bitmap makeWall(int t) {
        Bitmap b = blank(t);
        Canvas c = new Canvas(b);
        c.drawRect(0, 0, t, t, fill(0xFF6E6E76));
        float rh = t / 4f;
        float bw = t / 2f;
        int[] cols = {0xFFB5533F, 0xFFA04A39, 0xFFBD5C47};
        for (int i = 0; i < 4; i++) {
            float y = i * rh;
            float off = (i % 2 == 0) ? 0 : -bw / 2f;
            for (int k = 0; k < 3; k++) {
                float x = off + k * bw;
                c.drawRect(Math.max(0, x) + 0.6f, y + 0.6f, Math.min(t, x + bw) - 0.6f, y + rh - 0.6f,
                        fill(cols[(i + k) % cols.length]));
            }
        }
        return b;
    }

    private static Bitmap makeStone(int t) {
        Bitmap b = blank(t);
        Canvas c = new Canvas(b);
        float cx = t / 2f, cy = t / 2f, r = t * 0.47f;
        Paint p = fill(0xFF9A9AA2);
        p.setShader(new RadialGradient(cx - r * 0.35f, cy - r * 0.35f, r * 1.5f,
                0xFFD2D2DA, 0xFF63636B, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
        c.drawCircle(cx, cy, r, stroke(0xFF45454B, Math.max(1f, t / 18f)));
        return b;
    }

    private static Bitmap makeApple(int t) {
        Bitmap b = blank(t);
        Canvas c = new Canvas(b);
        float cx = t / 2f, cy = t * 0.56f, r = t * 0.36f;
        Paint p = fill(0xFFD32F2F);
        p.setShader(new RadialGradient(cx - r * 0.3f, cy - r * 0.3f, r * 1.4f,
                0xFFFF8A80, 0xFFB71C1C, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
        c.drawLine(cx, cy - r * 0.9f, cx + t * 0.06f, t * 0.12f, stroke(0xFF5D4037, Math.max(1.5f, t / 14f)));
        Path leaf = new Path();
        leaf.moveTo(cx + t * 0.05f, t * 0.2f);
        leaf.quadTo(cx + t * 0.25f, t * 0.04f, cx + t * 0.36f, t * 0.16f);
        leaf.quadTo(cx + t * 0.2f, t * 0.28f, cx + t * 0.05f, t * 0.2f);
        c.drawPath(leaf, fill(0xFF43A047));
        return b;
    }

    private static Bitmap makeBox(int t, int left) {
        Bitmap b = blank(t);
        Canvas c = new Canvas(b);
        int base = left == 3 ? 0xFFC58B3C : (left == 2 ? 0xFFAE7A34 : 0xFF946529);
        float m = t * 0.06f;
        RectF r = new RectF(m, m, t - m, t - m);
        c.drawRect(r, fill(base));
        Paint plank = stroke(0xFF6B4412, Math.max(1f, t / 14f));
        c.drawRect(r, plank);
        c.drawLine(r.left, r.top, r.right, r.bottom, stroke(0xFF8A5A1E, Math.max(1f, t / 16f)));
        c.drawLine(r.right, r.top, r.left, r.bottom, stroke(0xFF8A5A1E, Math.max(1f, t / 16f)));
        if (left < 3) {
            Paint crack = stroke(0xFF2B1A05, Math.max(1f, t / 22f));
            Path pa = new Path();
            pa.moveTo(t * 0.3f, t * 0.12f);
            pa.lineTo(t * 0.42f, t * 0.32f);
            pa.lineTo(t * 0.34f, t * 0.46f);
            c.drawPath(pa, crack);
            if (left < 2) {
                Path pb = new Path();
                pb.moveTo(t * 0.8f, t * 0.55f);
                pb.lineTo(t * 0.62f, t * 0.66f);
                pb.lineTo(t * 0.7f, t * 0.86f);
                c.drawPath(pb, crack);
            }
        }
        // verbleibende Treffer als helle Punkte
        float pr = Math.max(1.2f, t * 0.06f);
        for (int i = 0; i < left; i++) {
            float x = t / 2f + (i - (left - 1) / 2f) * t * 0.16f;
            c.drawCircle(x, t * 0.5f, pr, fill(0xFFFFF3C4));
        }
        return b;
    }

    private static Bitmap makeBomb(int t) {
        Bitmap b = blank(t);
        Canvas c = new Canvas(b);
        float cx = t / 2f, cy = t * 0.58f, r = t * 0.34f;
        Paint p = fill(0xFF222222);
        p.setShader(new RadialGradient(cx - r * 0.35f, cy - r * 0.35f, r * 1.4f,
                0xFF6B6B6B, 0xFF0A0A0A, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
        Path fuse = new Path();
        fuse.moveTo(cx + r * 0.45f, cy - r * 0.85f);
        fuse.quadTo(cx + r * 0.9f, cy - r * 1.3f, cx + r * 0.6f, cy - r * 1.55f);
        c.drawPath(fuse, stroke(0xFFBDBDBD, Math.max(1.5f, t / 14f)));
        c.drawCircle(cx + r * 0.6f, cy - r * 1.55f, Math.max(1.5f, t * 0.08f), fill(0xFFFF9800));
        c.drawCircle(cx + r * 0.6f, cy - r * 1.55f, Math.max(1f, t * 0.04f), fill(0xFFFFEB3B));
        return b;
    }

    private static Bitmap makeMatt(int t, boolean dead) {
        Bitmap b = blank(t);
        Canvas c = new Canvas(b);
        float cx = t / 2f, cy = t * 0.54f, r = t * 0.4f;
        c.drawCircle(cx, cy, r, fill(dead ? 0xFFB0A38A : 0xFFFFD54F));
        c.drawCircle(cx, cy, r, stroke(0xFF8D6E0B, Math.max(1f, t / 16f)));
        // Muetze
        Path cap = new Path();
        cap.moveTo(cx - r * 0.95f, cy - r * 0.25f);
        cap.quadTo(cx, cy - r * 1.6f, cx + r * 0.95f, cy - r * 0.25f);
        cap.close();
        c.drawPath(cap, fill(0xFF1E88E5));
        float ex = r * 0.38f, ey = cy - r * 0.05f, er = Math.max(1.5f, r * 0.24f);
        if (dead) {
            Paint x = stroke(0xFF222222, Math.max(1.2f, t / 14f));
            for (int s = -1; s <= 1; s += 2) {
                float px = cx + s * ex;
                c.drawLine(px - er, ey - er, px + er, ey + er, x);
                c.drawLine(px - er, ey + er, px + er, ey - er, x);
            }
            c.drawCircle(cx, cy + r * 0.5f, r * 0.2f, fill(0xFF5D1010));
        } else {
            for (int s = -1; s <= 1; s += 2) {
                float px = cx + s * ex;
                c.drawCircle(px, ey, er, fill(0xFFFFFFFF));
                c.drawCircle(px + er * 0.2f, ey, er * 0.5f, fill(0xFF222222));
            }
            RectF mouth = new RectF(cx - r * 0.5f, cy + r * 0.05f, cx + r * 0.5f, cy + r * 0.7f);
            c.drawArc(mouth, 20, 140, false, stroke(0xFF5D1010, Math.max(1.2f, t / 14f)));
        }
        return b;
    }
}
