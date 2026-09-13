package com.haseltonmediagroup.newtonscradle3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

public class CradleView extends View {
    private static final int BALLS = 5;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] angle = new float[BALLS];
    private final float[] omega = new float[BALLS];
    private final Vibrator vibrator;
    private long lastNs;
    private boolean running = true;
    private int activeBall = -1;
    private float lastTouchX;
    private long lastImpactMs;

    public CradleView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        linePaint.setStrokeWidth(4f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        lastNs = System.nanoTime();
    }

    public void resume() {
        running = true;
        lastNs = System.nanoTime();
        postInvalidateOnAnimation();
    }

    public void pause() {
        running = false;
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        drawBackground(c);
        if (running) updatePhysics();
        drawCradle(c);
        if (running) postInvalidateOnAnimation();
    }

    private void drawBackground(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        paint.setShader(new LinearGradient(0, 0, 0, h, 0xff10141c, 0xff020305, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        paint.setColor(0x22ffffff);
        c.drawCircle(w * .5f, h * .34f, Math.min(w, h) * .42f, paint);
    }

    private void drawCradle(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        float cx = w * .5f;
        float topY = h * .18f;
        float frameW = w * .78f;
        float frameH = h * .56f;
        float bottomY = topY + frameH;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff111317);
        paint.setShadowLayer(24f, 0, 12f, 0x88000000);
        c.drawRoundRect(cx - frameW * .52f, bottomY, cx + frameW * .52f, bottomY + h * .07f, 24, 24, paint);
        paint.clearShadowLayer();

        linePaint.setColor(0xffc8cdd6);
        linePaint.setStrokeWidth(Math.max(5f, w * .009f));
        c.drawLine(cx - frameW * .46f, topY, cx - frameW * .46f, bottomY, linePaint);
        c.drawLine(cx + frameW * .46f, topY, cx + frameW * .46f, bottomY, linePaint);
        c.drawLine(cx - frameW * .46f, topY, cx + frameW * .46f, topY, linePaint);

        float ballR = w * .072f;
        float spacing = ballR * 1.93f;
        float ropeLen = h * .36f;
        float startX = cx - spacing * 2f;

        for (int i = 0; i < BALLS; i++) {
            float anchorX = startX + spacing * i;
            float a = angle[i];
            float bx = anchorX + (float)Math.sin(a) * ropeLen;
            float by = topY + (float)Math.cos(a) * ropeLen;

            linePaint.setColor(0xff8f98a6);
            linePaint.setStrokeWidth(2.4f);
            c.drawLine(anchorX - ballR * .23f, topY + 8, bx - ballR * .23f, by - ballR * .15f, linePaint);
            c.drawLine(anchorX + ballR * .23f, topY + 8, bx + ballR * .23f, by - ballR * .15f, linePaint);

            paint.setShadowLayer(18f, 7f, 12f, 0x99000000);
            paint.setShader(new RadialGradient(bx - ballR * .32f, by - ballR * .34f, ballR * 1.25f,
                    new int[]{0xfff7f9fb, 0xff9ea7b2, 0xff3d4652, 0xff171b20},
                    new float[]{0f, .28f, .72f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(bx, by, ballR, paint);
            paint.setShader(null);
            paint.clearShadowLayer();

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(0x77ffffff);
            c.drawCircle(bx - ballR * .12f, by - ballR * .12f, ballR * .72f, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        paint.setColor(0x99ffffff);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(w * .048f);
        c.drawText("NEWTON'S CRADLE 3D", cx, h * .88f, paint);
        paint.setColor(0x66ffffff);
        paint.setTextSize(w * .032f);
        c.drawText("Drag an outer ball and release", cx, h * .925f, paint);
    }

    private void updatePhysics() {
        long now = System.nanoTime();
        float dt = Math.min(0.025f, Math.max(0.001f, (now - lastNs) / 1_000_000_000f));
        lastNs = now;
        float gOverL = 9.81f / 1.55f;

        for (int i = 0; i < BALLS; i++) {
            if (i == activeBall) continue;
            float alpha = -(float)Math.sin(angle[i]) * gOverL - omega[i] * .012f;
            omega[i] += alpha * dt;
            angle[i] += omega[i] * dt;
        }

        float contact = .018f;
        for (int i = 0; i < BALLS - 1; i++) {
            float relative = angle[i] - angle[i + 1];
            if (Math.abs(relative) < contact && omega[i] > omega[i + 1] + .02f) {
                float incoming = omega[i];
                omega[i] = omega[i + 1] * .985f;
                omega[i + 1] = incoming * .985f;
                impact(Math.min(1f, Math.abs(incoming) / 1.8f));
            }
        }
    }

    private void impact(float strength) {
        long now = SystemClock.uptimeMillis();
        if (now - lastImpactMs < 45 || strength < .09f) return;
        lastImpactMs = now;
        if (vibrator != null && vibrator.hasVibrator()) {
            long ms = strength > .6f ? 14 : 7;
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, 45 + (int)(strength * 90)));
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float w = getWidth();
        float h = getHeight();
        float cx = w * .5f;
        float topY = h * .18f;
        float ballR = w * .072f;
        float spacing = ballR * 1.93f;
        float startX = cx - spacing * 2f;
        float ropeLen = h * .36f;

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
                if (d < best && d < ballR * ballR * 3.3f) { best = d; nearest = i; }
            }
            activeBall = nearest;
            lastTouchX = e.getX();
            return true;
        }

        if (e.getActionMasked() == MotionEvent.ACTION_MOVE && activeBall >= 0) {
            float anchorX = startX + spacing * activeBall;
            float dx = e.getX() - anchorX;
            float newAngle = Math.max(-1.15f, Math.min(1.15f, dx / ropeLen));
            omega[activeBall] = (e.getX() - lastTouchX) / Math.max(1f, ropeLen) * 20f;
            angle[activeBall] = newAngle;
            lastTouchX = e.getX();
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
