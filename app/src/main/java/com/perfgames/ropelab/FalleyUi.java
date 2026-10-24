package com.perfgames.ropelab;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

/** Quiet shared surfaces and original, cosmetic-only chapter illustrations. UI thread only. */
public final class FalleyUi {
    public static final int BG = 0xff171b1c, PANEL = 0xff232b2c;
    public static final int MINT = 0xff70e2c2, GOLD = 0xfff6d477;
    public static final int TEXT = 0xffedf5f2, MUTED = 0xffa8b9b4, LINE = 0xff374744;
    private static final int CORAL = 0xffff897f, VIOLET = 0xffb8a3f4;
    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH = new Path();
    private static final RectF RECT = new RectF();
    private static final int[] ACCENTS = { MINT, GOLD, CORAL, VIOLET, GOLD, MINT };
    private static final LinearGradient[] SCENE_BACKGROUNDS = {
        gradient(0xff28413b), gradient(0xff3b3c30), gradient(0xff49332e),
        gradient(0xff363446), gradient(0xff3c3b30), gradient(0xff2e3d40)
    };

    /** Draws a quiet rounded surface; an emphasized card has a soft mint edge. */
    public static void card(Canvas c, float left, float top, float right, float bottom,
                            boolean emphasized) {
        if (right <= left || bottom <= top) return;
        fill(c, left, top, right, bottom, 20, emphasized ? 0xff243630 : PANEL);
        stroke(c, left, top, right, bottom, 20, emphasized ? 0xff416259 : 0xff2f3b39, 1);
    }

    /**
     * Text-free chapter scene, clipped to the supplied rounded rectangle. The illustration is
     * fitted at its natural 400:190 aspect, so characters retain their proportions at any size.
     * Motion moves by only a few logical pixels, never flashes, and freezes when reducedMotion.
     */
    public static void chapterScene(Canvas c, float left, float top, float right, float bottom,
                                    int chapter, int avatar, int palette,
                                    float animationTime, boolean reducedMotion) {
        float width = right - left, height = bottom - top;
        if (width <= 0 || height <= 0) return;
        int id = Math.max(0, Math.min(5, chapter));
        float time = safeTime(animationTime, reducedMotion);
        int accent = ACCENTS[id];
        c.save();
        PATH.reset(); RECT.set(left, top, right, bottom);
        PATH.addRoundRect(RECT, 16, 16, Path.Direction.CW);
        c.clipPath(PATH);
        fill(c, left, top, right, bottom, 0, 0xff202a29);
        float scale = Math.min(width / 400f, height / 190f);
        c.translate((left + right) * .5f, (top + bottom) * .5f);
        c.scale(scale, scale); c.translate(-200, -95);
        P.setStyle(Paint.Style.FILL); P.setShader(SCENE_BACKGROUNDS[id]);
        c.drawRect(0, 0, 400, 190, P); P.setShader(null);
        // A distant shaft, then quiet construction lines that give the drop depth.
        poly(c, 0xff1c2627, 111, 0, 289, 0, 318, 190, 82, 190);
        for (int i = 0; i < 7; i++) {
            float x = 80 + i * 40;
            line(c, x, 0, 200 + (x - 200) * 1.25f, 190, 0xff30413d, .8f);
        }
        for (int i = 0; i < 4; i++) line(c, 52, 22 + i * 43, 348, 22 + i * 43, 0xff2d3b38, .8f);
        line(c, 50, 16, 50, 175, 0xff527568, 2);
        line(c, 350, 16, 350, 175, 0xff527568, 2);
        fill(c, 39, 7, 61, 183, 8, 0xff263632);
        fill(c, 339, 7, 361, 183, 8, 0xff263632);
        for (int y = 24; y < 180; y += 38) {
            line(c, 43, y, 57, y, 0xff4b6458, 1.4f);
            line(c, 343, y, 357, y, 0xff4b6458, 1.4f);
        }
        switch (id) {
            case 1: spikes(c, time); break;
            case 2: fire(c, time); break;
            case 3: storm(c, time); break;
            case 4: foundry(c, time); break;
            case 5: core(c, time); break;
            default: lasers(c, time); break;
        }
        // The target is a promise of a safe landing, clearly separate from chapter hazards.
        fill(c, 70, 170, 330, 176, 3, 0xff40524c);
        fill(c, 169, 165, 231, 173, 4, MINT);
        oval(c, 185, 164, 215, 174, GOLD, false, 0);
        oval(c, 194, 166, 206, 171, 0xff243630, false, 0);
        float wave = (float)Math.sin(time * 1.55f), bob = reducedMotion ? 0 : (wave + 1) * 6;
        int rope = palette == 1 ? MINT : palette == 2 ? VIOLET : palette == 3 ? GOLD : accent;
        PATH.reset(); PATH.moveTo(56, 112);
        PATH.quadTo(200, 138 + wave * 3, 344, 112);
        pathStroke(c, PATH, alpha(rope, 30), 12);
        pathStroke(c, PATH, rope, 2.8f);
        circle(c, 56, 112, 4.5f, rope); circle(c, 344, 112, 4.5f, rope);
        // Ground shadow and actual approved shared avatar rig.
        oval(c, 181, 150, 219, 155, 0x30352b28, false, 0);
        AvatarRenderer.draw(c, 200, 103 - bob, 22, reducedMotion ? 0 : -90 * wave,
                            0, avatar, palette);
        c.restore();
    }

    /** A small selected-avatar preview, with an optional gentle floating pose. */
    public static void miniAvatar(Canvas c, float x, float y, float radius, int avatar,
                                  int palette, float animationTime, boolean reducedMotion) {
        if (radius <= 0) return;
        float wave = (float)Math.sin(safeTime(animationTime, reducedMotion) * 1.55f);
        float bob = reducedMotion ? 0 : radius * .06f * wave;
        AvatarRenderer.draw(c, x, y - bob, radius, reducedMotion ? 0 : -85 * wave,
                            0, avatar, palette);
    }

    /** Icon centred at x,y. size is the full square, independent of its touch target. */
    public static void icon(Canvas c, String kind, float x, float y, float size, int color) {
        if (size <= 0 || kind == null) return;
        c.save(); c.translate(x, y); c.scale(size / 24f, size / 24f);
        if ("play".equals(kind)) {
            poly(c, color, -6, -9, 9, 0, -6, 9);
        } else if ("collection".equals(kind) || "wardrobe".equals(kind)) {
            // A small display shelf, rather than a shopping icon for earned cosmetics.
            line(c, -9, 9, 9, 9, color, 1.8f);
            stroke(c, -9, -7, -2, 5, 2, color, 1.8f);
            stroke(c, 2, -9, 9, 5, 2, color, 1.8f);
            line(c, -5.5f, -4, -5.5f, 1, color, 1.5f);
            circle(c, 5.5f, -4.5f, 1.3f, color);
        } else if ("settings".equals(kind)) {
            line(c, -9, -6, 9, -6, color, 1.8f);
            line(c, -9, 0, 9, 0, color, 1.8f);
            line(c, -9, 6, 9, 6, color, 1.8f);
            fill(c, -5, -9, -1, -3, 1.5f, color);
            fill(c, 3, -3, 7, 3, 1.5f, color);
            fill(c, -6, 3, -2, 9, 1.5f, color);
        } else if ("chapters".equals(kind)) {
            stroke(c, -9, -9, 9, 9, 2, color, 1.8f);
            line(c, -4, -9, -4, 9, color, 1.8f);
            line(c, 0, -4, 5, -4, color, 1.8f);
            line(c, 0, 1, 5, 1, color, 1.8f);
        } else if ("daily".equals(kind)) {
            stroke(c, -9, -7, 9, 10, 3, color, 1.8f);
            line(c, -9, -2, 9, -2, color, 1.8f);
            line(c, -4, -10, -4, -5, color, 1.8f);
            line(c, 4, -10, 4, -5, color, 1.8f);
            poly(c, color, 0, 0, 1.2f, 3.2f, 4.5f, 3.5f, 2, 5.6f,
                 2.7f, 9, 0, 7.2f, -2.7f, 9, -2, 5.6f, -4.5f, 3.5f, -1.2f, 3.2f);
        } else if ("arcade".equals(kind)) {
            polyStroke(c, color, 1.8f, 2, -10, -7, 1, -1, 1, -3, 10, 8, -3, 2, -3);
        } else if ("back".equals(kind)) {
            line(c, 8, 0, -8, 0, color, 2);
            polyStrokeOpen(c, color, 2, -1, -7, -8, 0, -1, 7);
        } else if ("restart".equals(kind) || "retry".equals(kind)) {
            P.setStyle(Paint.Style.STROKE); P.setStrokeWidth(1.9f); P.setColor(color);
            P.setStrokeCap(Paint.Cap.ROUND); c.drawArc(-8, -8, 8, 8, -62, 288, false, P);
            polyStrokeOpen(c, color, 1.9f, -9, -2, -7, 5, -1, 3);
        } else if ("pause".equals(kind)) {
            fill(c, -7, -9, -2, 9, 1.3f, color);
            fill(c, 2, -9, 7, 9, 1.3f, color);
        } else if ("star".equals(kind)) {
            star(c, 0, 0, 10, color);
        } else if ("help".equals(kind)) {
            oval(c, -10, -10, 10, 10, color, true, 1.7f);
            polyStrokeOpen(c, color, 1.8f, -3, -4, -1, -6, 3, -5, 4, -2, 0, 1, 0, 3);
            circle(c, 0, 6, 1.1f, color);
        } else if ("language".equals(kind)) {
            oval(c, -9, -9, 9, 9, color, true, 1.7f);
            oval(c, -4, -9, 4, 9, color, true, 1.5f);
            line(c, -9, 0, 9, 0, color, 1.5f);
            line(c, -7, -5, 7, -5, color, 1.2f);
            line(c, -7, 5, 7, 5, color, 1.2f);
        }
        P.setStyle(Paint.Style.FILL); c.restore();
    }

    private static void lasers(Canvas c, float t) {
        float drift = (float)Math.sin(t * .6f) * 5;
        fill(c, 45, 39, 62, 63, 5, 0xff5c6356);
        fill(c, 338, 31, 355, 55, 5, 0xff5c6356);
        circle(c, 59, 51, 3, CORAL); circle(c, 341, 43, 3, CORAL);
        line(c, 64, 51, 147, 61 + drift, alpha(CORAL, 28), 11);
        line(c, 64, 51, 147, 61 + drift, CORAL, 1.5f);
        line(c, 336, 43, 268, 66 - drift, alpha(CORAL, 28), 11);
        line(c, 336, 43, 268, 66 - drift, CORAL, 1.5f);
        for (int i = 0; i < 3; i++) {
            float x = 84 + i * 15;
            line(c, x, 25, x + 7, 25, 0xff5b7970, 1.7f);
        }
    }
    private static void spikes(Canvas c, float t) {
        for (int i = 0; i < 4; i++) {
            float y = 31 + i * 29;
            poly(c, i % 2 == 0 ? 0xff8b937d : 0xff626f62, 58, y - 10, 79, y, 58, y + 10);
            poly(c, i % 2 == 0 ? 0xff626f62 : 0xff8b937d, 342, y - 10, 321, y, 342, y + 10);
            line(c, 59, y - 8, 76, y, 0xffb1b19a, .8f);
        }
        // A few distant canyon seams; no distracting movement in a stationary hazard.
        polyStrokeOpen(c, 0xff677361, 1.2f, 101, 22, 119, 38, 108, 52);
        polyStrokeOpen(c, 0xff677361, 1.2f, 295, 82, 282, 95, 291, 103);
    }
    private static void fire(Canvas c, float t) {
        for (int i = 0; i < 3; i++) {
            float x = 89 + i * 111;
            float y = (i == 1 ? 24 : 35) + (float)Math.sin(t * .8f + i) * 3;
            line(c, x + 13, y - 23, x + 2, y - 5, 0x335e4434, 12);
            poly(c, 0xffad614c, x - 8, y, x + 9, y - 27, x + 7, y + 6);
            circle(c, x, y, 9, CORAL); circle(c, x - 1, y + 2, 4.5f, GOLD);
        }
        line(c, 47, 151, 63, 151, CORAL, 2); line(c, 337, 151, 353, 151, CORAL, 2);
    }
    private static void storm(Canvas c, float t) {
        float sway = (float)Math.sin(t * .45f) * 3;
        cloud(c, 93 + sway, 30); cloud(c, 306 - sway, 40);
        polyStrokeOpen(c, alpha(VIOLET, 24), 10, 95, 44, 80, 66, 94, 62, 82, 84);
        polyStrokeOpen(c, VIOLET, 2, 95, 44, 80, 66, 94, 62, 82, 84);
        polyStrokeOpen(c, alpha(GOLD, 24), 10, 309, 54, 295, 77, 309, 73, 300, 94);
        polyStrokeOpen(c, GOLD, 2, 309, 54, 295, 77, 309, 73, 300, 94);
        for (int i = 0; i < 3; i++) line(c, 123 + i * 20, 22 + i * 4, 139 + i * 20, 22 + i * 4, 0xff645f7f, 1.5f);
    }
    private static void foundry(Canvas c, float t) {
        line(c, 60, 53, 119, 53, 0xff69725d, 6);
        line(c, 281, 70, 340, 70, 0xff69725d, 6);
        saw(c, 106, 53, 20, t * 10);
        saw(c, 294, 70, 24, -t * 9);
        for (int i = 0; i < 3; i++) {
            float y = 23 + i * 18;
            line(c, 136, y, 150, y + 7, 0xff5f6c5c, 1.3f);
            line(c, 264, y, 250, y + 7, 0xff5f6c5c, 1.3f);
        }
    }
    private static void core(Canvas c, float t) {
        float rotation = t * 4;
        c.save(); c.rotate(rotation, 200, 38);
        oval(c, 174, 12, 226, 64, 0xff607a73, true, 1.5f);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            circle(c, 200 + (float)Math.cos(a) * 26, 38 + (float)Math.sin(a) * 26, 3, i % 2 == 0 ? MINT : GOLD);
        }
        c.restore();
        oval(c, 184, 22, 216, 54, 0xff36544c, false, 0);
        oval(c, 191, 29, 209, 47, MINT, false, 0);
        oval(c, 196, 34, 204, 42, GOLD, false, 0);
        polyStrokeOpen(c, CORAL, 1.5f, 79, 45, 99, 45, 110, 60, 134, 60);
        polyStrokeOpen(c, VIOLET, 1.5f, 321, 42, 300, 42, 285, 70, 267, 70);
        circle(c, 79, 45, 4, CORAL); circle(c, 321, 42, 4, VIOLET);
    }
    private static void saw(Canvas c, float x, float y, float radius, float degrees) {
        c.save(); c.rotate(degrees, x, y);
        PATH.reset();
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI / 16;
            float r = radius * (i % 2 == 0 ? 1 : .8f);
            float px = x + (float)Math.cos(a) * r, py = y + (float)Math.sin(a) * r;
            if (i == 0) PATH.moveTo(px, py); else PATH.lineTo(px, py);
        }
        PATH.close(); P.setStyle(Paint.Style.FILL); P.setColor(0xff8a9484); c.drawPath(PATH, P);
        circle(c, x, y, radius * .58f, 0xff374640);
        oval(c, x - radius * .43f, y - radius * .43f, x + radius * .43f, y + radius * .43f, 0xff657666, true, 1.2f);
        circle(c, x, y, 3.3f, GOLD); c.restore();
    }
    private static void cloud(Canvas c, float x, float y) {
        circle(c, x - 10, y, 10, 0xff5e5878); circle(c, x, y - 5, 13, 0xff5e5878);
        circle(c, x + 13, y, 9, 0xff5e5878); fill(c, x - 13, y - 1, x + 16, y + 9, 4, 0xff5e5878);
    }
    private static LinearGradient gradient(int start) {
        return new LinearGradient(0, 0, 320, 190, start, 0xff20272b, Shader.TileMode.CLAMP);
    }
    private static float safeTime(float time, boolean reduced) {
        return reduced || Float.isNaN(time) || Float.isInfinite(time) ? 0 : time % 10000f;
    }
    private static int alpha(int color, int opacity) { return (color & 0x00ffffff) | ((opacity & 255) << 24); }
    private static void fill(Canvas c, float l, float t, float r, float b, float radius, int color) {
        P.setStyle(Paint.Style.FILL); P.setColor(color); P.setShader(null);
        c.drawRoundRect(l, t, r, b, radius, radius, P);
    }
    private static void stroke(Canvas c, float l, float t, float r, float b, float radius, int color, float width) {
        P.setStyle(Paint.Style.STROKE); P.setStrokeWidth(width); P.setColor(color);
        c.drawRoundRect(l, t, r, b, radius, radius, P); P.setStyle(Paint.Style.FILL);
    }
    private static void line(Canvas c, float x, float y, float xx, float yy, int color, float width) {
        P.setStyle(Paint.Style.STROKE); P.setStrokeCap(Paint.Cap.ROUND); P.setStrokeWidth(width); P.setColor(color);
        c.drawLine(x, y, xx, yy, P); P.setStyle(Paint.Style.FILL);
    }
    private static void circle(Canvas c, float x, float y, float r, int color) {
        P.setStyle(Paint.Style.FILL); P.setColor(color); c.drawCircle(x, y, r, P);
    }
    private static void oval(Canvas c, float l, float t, float r, float b, int color, boolean outline, float width) {
        P.setStyle(outline ? Paint.Style.STROKE : Paint.Style.FILL); P.setColor(color); P.setStrokeWidth(width);
        c.drawOval(l, t, r, b, P); P.setStyle(Paint.Style.FILL);
    }
    private static void poly(Canvas c, int color, float... points) {
        PATH.reset(); PATH.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) PATH.lineTo(points[i], points[i + 1]);
        PATH.close(); P.setStyle(Paint.Style.FILL); P.setColor(color); c.drawPath(PATH, P);
    }
    private static void pathStroke(Canvas c, Path path, int color, float width) {
        P.setStyle(Paint.Style.STROKE); P.setStrokeCap(Paint.Cap.ROUND); P.setStrokeJoin(Paint.Join.ROUND);
        P.setColor(color); P.setStrokeWidth(width); c.drawPath(path, P); P.setStyle(Paint.Style.FILL);
    }
    private static void polyStroke(Canvas c, int color, float width, float... points) {
        polygonStroke(c, color, width, true, points);
    }
    private static void polyStrokeOpen(Canvas c, int color, float width, float... points) {
        polygonStroke(c, color, width, false, points);
    }
    private static void polygonStroke(Canvas c, int color, float width, boolean closed, float... points) {
        PATH.reset(); PATH.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) PATH.lineTo(points[i], points[i + 1]);
        if (closed) PATH.close(); pathStroke(c, PATH, color, width);
    }
    private static void star(Canvas c, float x, float y, float radius, int color) {
        PATH.reset();
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float r = radius * (i % 2 == 0 ? 1 : .44f);
            float px = x + (float)Math.cos(a) * r, py = y + (float)Math.sin(a) * r;
            if (i == 0) PATH.moveTo(px, py); else PATH.lineTo(px, py);
        }
        PATH.close(); P.setStyle(Paint.Style.FILL); P.setColor(color); c.drawPath(PATH, P);
    }
    private FalleyUi() { }
}
