package com.haseltonmediagroup.newtonscradle3d;

import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class MainActivity extends AndroidApplication implements NewtonsCradleGame.PlatformBridge {
    private Vibrator vibrator;
    private long lastImpactMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        cfg.useImmersiveMode = true;
        cfg.useAccelerometer = false;
        cfg.useCompass = false;

        View gameView = initializeForView(new NewtonsCradleGame(this), cfg);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(51,54,59));
        root.addView(gameView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText("NEWTON'S CRADLE");
        title.setTextColor(Color.rgb(238,240,244));
        title.setTextSize(21);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(.18f);
        title.setShadowLayer(7f,0f,2f,Color.argb(150,0,0,0));
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(64), Gravity.TOP);
        titleParams.topMargin=dp(12);
        root.addView(title,titleParams);

        // Production AdMob's AdView is inserted into this reserved container.
        FrameLayout adContainer = new FrameLayout(this);
        adContainer.setId(View.generateViewId());
        adContainer.setBackgroundColor(Color.rgb(18,19,22));
        TextView adLabel = new TextView(this);
        adLabel.setText("ADVERTISEMENT");
        adLabel.setTextColor(Color.rgb(112,115,122));
        adLabel.setTextSize(9);
        adLabel.setGravity(Gravity.CENTER);
        adContainer.addView(adLabel,new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,dp(50),Gravity.BOTTOM);
        root.addView(adContainer,adParams);
        setContentView(root);
    }

    private int dp(int value){ return Math.round(value*getResources().getDisplayMetrics().density); }

    @Override
    public void impact(float strength) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastImpactMs < 55) return;
        lastImpactMs = now;
        if (vibrator != null && vibrator.hasVibrator()) {
            long ms = strength > 0.7f ? 24 : (strength > 0.35f ? 16 : 10);
            int amplitude = Math.min(210, 80 + Math.round(strength * 130));
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, amplitude));
            } else {
                vibrator.vibrate(ms);
            }
        }
    }
}
