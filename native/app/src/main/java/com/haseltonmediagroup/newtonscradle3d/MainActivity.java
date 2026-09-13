package com.haseltonmediagroup.newtonscradle3d;

import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AndroidApplication implements NewtonsCradleGame.PlatformBridge {
    private Vibrator vibrator;
    private AdView adView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        cfg.useImmersiveMode = true;
        cfg.useAccelerometer = false;
        cfg.useCompass = false;

        // Initialize LibGDX first so AndroidApplication owns a valid GL surface/lifecycle.
        View gameView = initializeForView(new NewtonsCradleGame(this), cfg);
        FrameLayout root = new FrameLayout(this);
        root.addView(gameView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // AdMob is deliberately initialized after the game view. A failed ad must never
        // prevent the physics toy from opening.
        root.post(() -> {
            try {
                MobileAds.initialize(getApplicationContext(), status -> {});
                adView = new AdView(this);
                adView.setAdSize(AdSize.BANNER);
                adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
                FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                root.addView(adView, adParams);
                adView.loadAd(new AdRequest.Builder().build());
            } catch (Throwable ignored) {
                // Keep the cradle usable even when Play Services/AdMob is unavailable.
            }
        });
    }

    @Override protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (adView != null) adView.resume();
    }

    @Override protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }

    @Override public void impact(float strength) {
        if (vibrator != null && vibrator.hasVibrator()) {
            long ms = strength > 0.7f ? 18 : 8;
            if (android.os.Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(ms, 70));
            else vibrator.vibrate(ms);
        }
    }
}
