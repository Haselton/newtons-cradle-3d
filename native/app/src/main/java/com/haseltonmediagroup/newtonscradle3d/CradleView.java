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
        lastNs = System.nanoTime();
        setKeepScreenOn(true);
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
        drawBackground(c);
        if (running) updatePhysics();
        drawCradle(c);
        if (running) postInvalidateOnAnimation();
    }

    private void drawBackground(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xff151b24, 0xff0a0e14, 0xff030405},
                new float[]{0f, .52f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        paint.setShader(new RadialGradient(w * .5f, h * .34f, w * .58f,
                new int[]{0x334f6c91, 0x11243349, 0x00000000},
                new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(w * .5f, h * .34f, w * .58f, paint);
        paint.setShader(null);
    }

    private void drawCradle(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        float cx = w * .5f;
        float topY = h * .17f;
        float baseTop = h * .735f;
        float frameHalf = w * .39f;
        float depthX = w * .045f;
        float depthY = h * .026f;

        drawFloorShadow(c, cx, baseTop, w, h);
        drawBase(c, cx, baseTop, w, h, depthX, depthY);

        // Rear frame first for depth.
        drawMetalLine(c, cx - frameHalf + depthX, topY + depthY,
                cx - frameHalf + depthX, baseTop + depthY, w * .014f, true);
        drawMetalLine(c, cx + frameHalf + depthX, topY + depthY,
                cx + frameHalf + depthX, baseTop + depthY, w * .014f, true);
        drawMetalLine(c, cx - frameHalf + depthX, topY + depthY,
                cx + frameHalf + depthX, topY + depthY, w * .016f, true);

        float ballR = w * .069f;
        float spacing = ballR * 2.015f;
        float ropeLen = h * .365f;
        float startX = cx - spacing * 2f;

        // Back wires.
        for (int i = 0; i < BALLS; i++) {
            float anchorX = startX + spacing * i;
            float a = angle[i];
            float bx = anchorX + (float)Math.sin(a) * ropeLen;
            float by = topY + (float)Math.cos(a) * ropeLen;
            linePaint.setColor(0xff626b76);
            linePaint.setStrokeWidth(Math.max(1.4f, w * .0025f));
            c.drawLine(anchorX + ballR * .24f + depthX * .45f, topY + depthY,
                    bx + ballR * .24f, by - ballR * .18f, linePaint);
        }

        // Front wires and balls.
        for (int i = 0; i < BALLS; i++) {
            float anchorX = startX + spacing * i;
            float a = angle[i];
            float bx = anchorX + (float)Math.sin(a) * ropeLen;
            float by = topY + (float)Math.cos(a) * ropeLen;

            linePaint.setColor(0xffc3c9d1);
            linePaint.setStrokeWidth(Math.max(1.6f, w * .0028f));
            c.drawLine(anchorX - ballR * .24f, topY + 4f,
                    bx - ballR * .24f, by - ballR * .18f, linePaint);

            drawBallShadow(c, bx, by, ballR);
            drawSteelBall(c, bx, by, ballR);
        }

        // Front frame overlays the scene and creates believable occlusion.
        drawMetalLine(c, cx - frameHalf, topY,
                cx - frameHalf, baseTop, w * .015f, false);
        drawMetalLine(c, cx + frameHalf, topY,
                cx + frameHalf, baseTop, w * .015f, false);
        drawMetalLine(c, cx - frameHalf, topY,
                cx + frameHalf, topY, w * .017f, false);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xccf4f6f8);
        paint.setTextSize(w * .046f);
        paint.setFakeBoldText(true);
        c.drawText("NEWTON'S CRADLE", cx, h * .865f, paint);
        paint.setFakeBoldText(false);
        paint.setColor(0x88d8dde5);
        paint.setTextSize(w * .029f);
        c.drawText("Pull either outer ball and release", cx, h * .91f, paint);
    }

    private void drawFloorShadow(Canvas c, float cx, float baseTop, float w, float h) {
        paint.setShader(new RadialGradient(cx, baseTop + h * .065f, w * .48f,
                new int[]{0x66000000, 0x22000000, 0x00000000},
                new float[]{0f, .55f, 1f}, Shader.TileMode.CLAMP));
        c.save();
        c.scale(1f, .26f, cx, baseTop + h * .065f);
        c.drawCircle(cx, baseTop + h * .065f, w * .48f, paint);
        c.restore();
        paint.setShader(null);
    }

    private void drawBase(Canvas c, float cx, float y, float w, float h, float dx, float dy) {
        float half = w * .43f;
        float bh = h * .07f;

        Path top = new Path();
        top.moveTo(cx - half, y);
        top.lineTo(cx + half, y);
        top.lineTo(cx + half + dx, y + dy);
        top.lineTo(cx - half + dx, y + dy);
        top.close();
        paint.setShader(new LinearGradient(cx - half, y, cx + half, y + dy,
                new int[]{0xff2c323a, 0xff12161c, 0xff3a414a}, null, Shader.TileMode.CLAMP));
        c.drawPath(top, paint);

        Path front = new Path();
        front.moveTo(cx - half, y);
        front.lineTo(cx + half, y);
        front.lineTo(cx + half, y + bh);
        front.lineTo(cx - half, y + bh);
        front.close();
        paint.setShader(new LinearGradient(0, y, 0, y + bh,
                0xff20252c, 0xff080a0d, Shader.TileMode.CLAMP));
        c.drawPath(front, paint);
        paint.setShader(null);

        linePaint.setColor(0x667b8490);
        linePaint.setStrokeWidth(1.5f);
        c.drawLine(cx - half + w * .018f, y + 3f, cx + half - w * .018f, y + 3f, linePaint);
    }

    private void drawMetalLine(Canvas c, float x1, float y1, float x2, float y2, float width, boolean rear) {
        linePaint.setStrokeWidth(width);
        linePaint.setShader(new LinearGradient(x1, y1, x2 + 1f, y2 + 1f,
                rear ? new int[]{0xff48515c, 0xff1d232a, 0xff65707d}
                     : new int[]{0xffd9dee4, 0xff59636f, 0xffeef1f4},
                null, Shader.TileMode.CLAMP));
        c.drawLine(x1, y1, x2, y2, linePaint);
        linePaint.setShader(null);
    }

    private void drawBallShadow(Canvas c, float x, float y, float r) {
        paint.setShader(new RadialGradient(x + r * .18f, y + r * .38f, r * 1.12f,
                new int[]{0x88000000, 0x33000000, 0x00000000},
                new float[]{0f, .55f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(x + r * .17f, y + r * .28f, r * 1.12f, paint);
        paint.setShader(null);
    }

    private void drawSteelBall(Canvas c, float x, float y, float r) {
        paint.setShader(new RadialGradient(x - r * .34f, y - r * .42f, r * 1.5f,
                new int[]{0xffffffff, 0xffdce2e8, 0xff89939e, 0xff313942, 0xff0e1115},
                new float[]{0f, .16f, .42f, .76f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r, paint);
        paint.setShader(null);

        // Cool reflection band and small hard highlight make the sphere read as chrome.
        paint.setColor(0x55dbeeff);
        c.drawOval(x - r * .62f, y - r * .30f, x + r * .46f, y - r * .10f, paint);
        paint.setColor(0xaaffffff);
        c.drawCircle(x - r * .35f, y - r * .38f, r * .11f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.2f, r * .035f));
        paint.setColor(0x66ffffff);
        c.drawCircle(x, y, r * .94f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void updatePhysics() {
        long now = System.nanoTime();
        float dt = Math.min(.030f, Math.max(.001f, (now - lastNs) / 1_000_000_000f));
        lastNs = now;

        int subSteps = 4;
        float h = dt / subSteps;
        for (int s = 0; s < subSteps; s++) {
            // Only the two outside balls need to swing for classic one-ball-in/one-ball-out behavior.
            stepPendulum(0, h);
            stepPendulum(BALLS - 1, h);

            // Keep the center transfer balls seated together instead of wobbling independently.
            for (int i = 1; i < BALLS - 1; i++) {
                angle[i] = 0f;
                omega[i] = 0f;
            }

            if (activeBall != 0 && angle[0] >= -0.010f && omega[0] > .04f) {
                float incoming = omega[0];
                angle[0] = 0f;
                omega[0] = 0f;
                angle[BALLS - 1] = 0f;
                omega[BALLS - 1] = incoming * .993f;
                impact(Math.min(1f, Math.abs(incoming) / 2.0f));
            }

            if (activeBall != BALLS - 1 && angle[BALLS - 1] <= .010f && omega[BALLS - 1] < -.04f) {
                float incoming = omega[BALLS - 1];
                angle[BALLS - 1] = 0f;
                omega[BALLS - 1] = 0f;
                angle[0] = 0f;
                omega[0] = incoming * .993f;
                impact(Math.min(1f, Math.abs(incoming) / 2.0f));
            }
        }
    }

    private void stepPendulum(int i, float dt) {
        if (i == activeBall) return;
        float alpha = -(9.81f / 1.70f) * (float)Math.sin(angle[i]) - omega[i] * .009f;
        omega[i] += alpha * dt;
        angle[i] += omega[i] * dt;
        if (Math.abs(angle[i]) < .0004f && Math.abs(omega[i]) < .003f) {
            angle[i] = 0f;
            omega[i] = 0f;
        }
    }

    private void impact(float strength) {
        long now = SystemClock.uptimeMillis();
        if (now - lastImpactMs < 80 || strength < .08f) return;
        lastImpactMs = now;

        if (impactListener != null) impactListener.onImpact(strength);

        if (vibrator != null && vibrator.hasVibrator() && android.os.Build.VERSION.SDK_INT >= 26) {
            int amplitude = 45 + (int)(strength * 90f);
            vibrator.vibrate(VibrationEffect.createOneShot(strength > .58f ? 13 : 7, amplitude));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float w = getWidth();
        float h = getHeight();
        float cx = w * .5f;
        float topY = h * .17f;
        float ballR = w * .069f;
        float spacing = ballR * 2.015f;
        float startX = cx - spacing * 2f;
        float ropeLen = h * .365f;

        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            int nearest = -1;
            float best = Float.MAX_VALUE;
            for (int i : new int[]{0, BALLS - 1}) {
                float anchorX = startX + spacing * i;
                float bx = anchorX + (float)Math.sin(angle[i]) * ropeLen;
                float by = topY + (float)Math.cos(angle[i]) * ropeLen;
                float dx = e.getX() - bx;
                float dy = e.getY() - by;
                float d = dx * dx + dy * dy;
                if (d < best && d < ballR * ballR * 4.0f) {
                    best = d;
                    nearest = i;
                }
            }
            activeBall = nearest;
            if (activeBall >= 0) omega[activeBall] = 0f;
            return true;
        }

        if (e.getActionMasked() == MotionEvent.ACTION_MOVE && activeBall >= 0) {
            float anchorX = startX + spacing * activeBall;
            float ratio = Math.max(-.91f, Math.min(.91f, (e.getX() - anchorX) / ropeLen));
            float a = (float)Math.asin(ratio);
            if (activeBall == 0) a = Math.min(0f, a);
            else a = Math.max(0f, a);
            angle[activeBall] = Math.max(-1.10f, Math.min(1.10f, a));
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
