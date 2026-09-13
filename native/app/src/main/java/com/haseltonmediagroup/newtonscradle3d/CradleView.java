package com.haseltonmediagroup.newtonscradle3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

public class CradleView extends View {
    public interface ImpactListener { void onImpact(float strength); }

    private static final int BALLS = 5;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] angle = new float[BALLS];
    private final float[] omega = new float[BALLS];
    private final Vibrator vibrator;
    private final ImpactListener impactListener;

    private long lastNs;
    private long lastImpactMs;
    private boolean running = true;
    private int activeBall = -1;

    public CradleView(Context context, ImpactListener impactListener) {
        super(context);
        this.impactListener = impactListener;
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        setKeepScreenOn(true);
        lastNs = System.nanoTime();
    }

    public void resume() {
        running = true;
        lastNs = System.nanoTime();
        postInvalidateOnAnimation();
    }

    public void pause() { running = false; }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth();
        float h = getHeight();
        drawRoom(c, w, h);
        if (running) updatePhysics();
        drawCradle(c, w, h);
        if (running) postInvalidateOnAnimation();
    }

    private void drawRoom(Canvas c, float w, float h) {
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xff15100c, 0xff090b0f, 0xff020305},
                new float[]{0f, .54f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        paint.setShader(new RadialGradient(w * .74f, h * .28f, w * .68f,
                new int[]{0x335f4630, 0x14261d17, 0x00000000},
                new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(w * .74f, h * .28f, w * .68f, paint);
        paint.setShader(null);

        paint.setShader(new LinearGradient(0, h * .70f, 0, h,
                new int[]{0xff151312, 0xff050506}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, h * .70f, w, h, paint);
        paint.setShader(null);

        // Soft reflection pool beneath the base.
        paint.setShader(new RadialGradient(w * .5f, h * .80f, w * .42f,
                new int[]{0x223e2d1f, 0x11000000, 0x00000000}, null, Shader.TileMode.CLAMP));
        c.save();
        c.scale(1f, .23f, w * .5f, h * .80f);
        c.drawCircle(w * .5f, h * .80f, w * .42f, paint);
        c.restore();
        paint.setShader(null);
    }

    private void drawCradle(Canvas c, float w, float h) {
        float cx = w * .5f;
        float topY = h * .175f;
        float baseTop = h * .705f;
        float frameHalf = w * .355f;
        float depthX = w * .038f;
        float depthY = h * .018f;

        drawWoodBase(c, cx, baseTop, w, h, depthX, depthY);

        // Rear chrome rails for depth.
        drawChromeRail(c, cx - frameHalf + depthX, topY + depthY,
                cx - frameHalf + depthX, baseTop + depthY, w * .013f, true);
        drawChromeRail(c, cx + frameHalf + depthX, topY + depthY,
                cx + frameHalf + depthX, baseTop + depthY, w * .013f, true);
        drawChromeRail(c, cx - frameHalf + depthX, topY + depthY,
                cx + frameHalf + depthX, topY + depthY, w * .015f, true);

        float ballR = w * .058f;
        float spacing = ballR * 2.02f;
        float ropeLen = h * .342f;
        float startX = cx - spacing * 2f;

        // Back suspension wires.
        for (int i = 0; i < BALLS; i++) {
            float anchorX = startX + spacing * i;
            float a = angle[i];
            float bx = anchorX + (float)Math.sin(a) * ropeLen;
            float by = topY + (float)Math.cos(a) * ropeLen;
            linePaint.setColor(0xff5d6268);
            linePaint.setStrokeWidth(Math.max(1.2f, w * .0022f));
            c.drawLine(anchorX + ballR * .22f + depthX * .50f, topY + depthY,
                    bx + ballR * .22f, by - ballR * .17f, linePaint);
        }

        // Front wires and balls.
        for (int i = 0; i < BALLS; i++) {
            float anchorX = startX + spacing * i;
            float a = angle[i];
            float bx = anchorX + (float)Math.sin(a) * ropeLen;
            float by = topY + (float)Math.cos(a) * ropeLen;

            linePaint.setColor(0xffc7c7c5);
            linePaint.setStrokeWidth(Math.max(1.4f, w * .0025f));
            c.drawLine(anchorX - ballR * .22f, topY + 3f,
                    bx - ballR * .22f, by - ballR * .17f, linePaint);

            drawSphereShadow(c, bx, by, ballR);
            drawChromeSphere(c, bx, by, ballR);
        }

        // Front chrome rails create believable occlusion.
        drawChromeRail(c, cx - frameHalf, topY, cx - frameHalf, baseTop, w * .014f, false);
        drawChromeRail(c, cx + frameHalf, topY, cx + frameHalf, baseTop, w * .014f, false);
        drawChromeRail(c, cx - frameHalf, topY, cx + frameHalf, topY, w * .016f, false);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xffe8dfd2);
        paint.setTextSize(w * .050f);
        paint.setFakeBoldText(true);
        c.drawText("NEWTON'S CRADLE 3D", cx, h * .855f, paint);
        paint.setFakeBoldText(false);
        paint.setColor(0xffb89a72);
        paint.setTextSize(w * .027f);
        c.drawText("RELAX  •  FOCUS  •  BALANCE", cx, h * .895f, paint);
        paint.setColor(0x99ffffff);
        paint.setTextSize(w * .027f);
        c.drawText("Grab either outer ball and release", cx, h * .940f, paint);
    }

    private void drawWoodBase(Canvas c, float cx, float y, float w, float h, float dx, float dy) {
        float half = w * .405f;
        float bh = h * .072f;

        Path top = new Path();
        top.moveTo(cx - half, y);
        top.lineTo(cx + half, y);
        top.lineTo(cx + half + dx, y + dy);
        top.lineTo(cx - half + dx, y + dy);
        top.close();
        paint.setShader(new LinearGradient(cx - half, y, cx + half, y + dy,
                new int[]{0xff3a2113, 0xff6b3c1e, 0xff2a160d}, null, Shader.TileMode.CLAMP));
        c.drawPath(top, paint);

        paint.setShader(new LinearGradient(0, y, 0, y + bh,
                new int[]{0xff5b321a, 0xff2f180d, 0xff160b07}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(cx - half, y, cx + half, y + bh, w * .018f, w * .018f, paint);
        paint.setShader(null);

        // Fine wood grain.
        linePaint.setStrokeWidth(1.1f);
        linePaint.setColor(0x334f2815);
        for (int i = 0; i < 8; i++) {
            float yy = y + bh * (.18f + i * .085f);
            c.drawLine(cx - half + w * .02f, yy, cx + half - w * .02f, yy + (i % 2 == 0 ? 2f : -2f), linePaint);
        }

        // Brass name plate.
        float pw = w * .34f;
        float ph = h * .027f;
        paint.setShader(new LinearGradient(cx - pw / 2f, y + bh * .35f, cx + pw / 2f, y + bh * .35f,
                new int[]{0xff8b6a40, 0xffd4b37a, 0xff7d5b35}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(cx - pw / 2f, y + bh * .30f, cx + pw / 2f, y + bh * .30f + ph, 8f, 8f, paint);
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xff20160f);
        paint.setTextSize(w * .020f);
        c.drawText("NEWTON'S CRADLE", cx, y + bh * .30f + ph * .72f, paint);
    }

    private void drawChromeRail(Canvas c, float x1, float y1, float x2, float y2, float width, boolean rear) {
        linePaint.setStrokeWidth(width);
        linePaint.setShader(new LinearGradient(x1, y1, x2 + 1f, y2 + 1f,
                rear ? new int[]{0xff5f5a54, 0xff2c2d30, 0xff8b8378}
                     : new int[]{0xfff2eee8, 0xff8f9298, 0xff34383d, 0xfffaf8f2},
                null, Shader.TileMode.CLAMP));
        c.drawLine(x1, y1, x2, y2, linePaint);
        linePaint.setShader(null);
    }

    private void drawSphereShadow(Canvas c, float x, float y, float r) {
        paint.setShader(new RadialGradient(x + r * .18f, y + r * .30f, r * 1.15f,
                new int[]{0x77000000, 0x22000000, 0x00000000}, null, Shader.TileMode.CLAMP));
        c.drawCircle(x + r * .17f, y + r * .28f, r * 1.15f, paint);
        paint.setShader(null);
    }

    private void drawChromeSphere(Canvas c, float x, float y, float r) {
        paint.setShader(new RadialGradient(x - r * .34f, y - r * .39f, r * 1.48f,
                new int[]{0xffffffff, 0xffeee9df, 0xffb8bdc3, 0xff737982, 0xff252a30, 0xff080a0d},
                new float[]{0f, .10f, .28f, .48f, .76f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r, paint);
        paint.setShader(null);

        // Warm room reflection band.
        paint.setShader(new LinearGradient(x, y - r * .25f, x, y + r * .22f,
                new int[]{0x10ffffff, 0x66583a22, 0x22301e13, 0x08ffffff},
                new float[]{0f, .36f, .66f, 1f}, Shader.TileMode.CLAMP));
        c.drawOval(x - r * .82f, y - r * .20f, x + r * .82f, y + r * .22f, paint);
        paint.setShader(null);

        paint.setColor(0xccffffff);
        c.drawCircle(x - r * .34f, y - r * .38f, r * .105f, paint);
        paint.setColor(0x55ffffff);
        c.drawCircle(x - r * .16f, y - r * .20f, r * .060f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.0f, r * .035f));
        paint.setColor(0x55ffffff);
        c.drawCircle(x, y, r * .95f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void updatePhysics() {
        long now = System.nanoTime();
        float dt = Math.min(.025f, Math.max(.001f, (now - lastNs) / 1_000_000_000f));
        lastNs = now;

        int subSteps = 6;
        float h = dt / subSteps;
        for (int s = 0; s < subSteps; s++) {
            stepPendulum(0, h);
            stepPendulum(BALLS - 1, h);

            // Center balls stay seated and visually touching; energy is transferred through them.
            for (int i = 1; i < BALLS - 1; i++) {
                angle[i] = 0f;
                omega[i] = 0f;
            }

            if (activeBall != 0 && angle[0] >= -0.006f && omega[0] > .035f) {
                float incoming = omega[0];
                angle[0] = 0f;
                omega[0] = 0f;
                angle[BALLS - 1] = 0f;
                omega[BALLS - 1] = incoming * .988f;
                impact(Math.min(1f, Math.abs(incoming) / 1.85f));
            }

            if (activeBall != BALLS - 1 && angle[BALLS - 1] <= .006f && omega[BALLS - 1] < -.035f) {
                float incoming = omega[BALLS - 1];
                angle[BALLS - 1] = 0f;
                omega[BALLS - 1] = 0f;
                angle[0] = 0f;
                omega[0] = incoming * .988f;
                impact(Math.min(1f, Math.abs(incoming) / 1.85f));
            }
        }
    }

    private void stepPendulum(int i, float dt) {
        if (i == activeBall) return;
        float alpha = -(9.81f / 1.85f) * (float)Math.sin(angle[i]) - omega[i] * .014f;
        omega[i] += alpha * dt;
        angle[i] += omega[i] * dt;
        if (Math.abs(angle[i]) < .00025f && Math.abs(omega[i]) < .0025f) {
            angle[i] = 0f;
            omega[i] = 0f;
        }
    }

    private void impact(float strength) {
        long now = SystemClock.uptimeMillis();
        if (now - lastImpactMs < 72 || strength < .07f) return;
        lastImpactMs = now;

        if (impactListener != null) impactListener.onImpact(strength);

        if (vibrator != null && vibrator.hasVibrator() && android.os.Build.VERSION.SDK_INT >= 26) {
            int amplitude = 38 + (int)(strength * 100f);
            vibrator.vibrate(VibrationEffect.createOneShot(strength > .55f ? 12 : 6, amplitude));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float w = getWidth();
        float h = getHeight();
        float cx = w * .5f;
        float topY = h * .175f;
        float ballR = w * .058f;
        float spacing = ballR * 2.02f;
        float startX = cx - spacing * 2f;
        float ropeLen = h * .342f;

        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            int nearest = -1;
            float best = Float.MAX_VALUE;
            float touchRadius = ballR * 3.15f;

            for (int i : new int[]{0, BALLS - 1}) {
                float anchorX = startX + spacing * i;
                float bx = anchorX + (float)Math.sin(angle[i]) * ropeLen;
                float by = topY + (float)Math.cos(angle[i]) * ropeLen;
                float dx = e.getX() - bx;
                float dy = e.getY() - by;
                float d2 = dx * dx + dy * dy;
                if (d2 < best && d2 < touchRadius * touchRadius) {
                    best = d2;
                    nearest = i;
                }
            }

            // Extra thumb lanes make the end balls easy to grab even when the thumb obscures them.
            float restingY = topY + ropeLen;
            if (nearest < 0 && Math.abs(e.getY() - restingY) < ballR * 2.4f) {
                if (e.getX() < w * .27f) nearest = 0;
                else if (e.getX() > w * .73f) nearest = BALLS - 1;
            }

            activeBall = nearest;
            if (activeBall >= 0) omega[activeBall] = 0f;
            return true;
        }

        if (e.getActionMasked() == MotionEvent.ACTION_MOVE && activeBall >= 0) {
            float anchorX = startX + spacing * activeBall;
            float ratio = Math.max(-.90f, Math.min(.90f, (e.getX() - anchorX) / ropeLen));
            float a = (float)Math.asin(ratio);
            if (activeBall == 0) a = Math.min(0f, a);
            else a = Math.max(0f, a);
            angle[activeBall] = Math.max(-1.08f, Math.min(1.08f, a));
            omega[activeBall] = 0f;
            invalidate();
            return true;
        }

        if (e.getActionMasked() == MotionEvent.ACTION_UP || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            activeBall = -1;
            return true;
        }
        return true;
    }
}
