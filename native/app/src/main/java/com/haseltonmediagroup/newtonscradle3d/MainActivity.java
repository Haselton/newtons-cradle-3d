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

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        cfg.useImmersiveMode = true;
        cfg.useAccelerometer = false;
        cfg.useCompass = false;

        View gameView = initializeForView(new NewtonsCradleGame(this), cfg);
        FrameLayout root = new FrameLayout(this);
        root.addView(gameView, new FrameLayout.LayoutParams(-1, -1));

        MobileAds.initialize(this, status -> {});
        AdView ad = new AdView(this);
        ad.setAdSize(AdSize.BANNER);
        ad.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
        ad.loadAd(new AdRequest.Builder().build());

        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        root.addView(ad, adParams);
        setContentView(root);
    }

    @Override public void impact(float strength) {
        if (vibrator != null && vibrator.hasVibrator()) {
            long ms = strength > 0.7f ? 18 : 8;
            if (android.os.Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(ms, 70));
            else vibrator.vibrate(ms);
        }
    }
}
