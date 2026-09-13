package com.haseltonmediagroup.newtonscradle3d;

import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;

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

        // Minimal LibGDX startup path. Keep all third-party SDK initialization out
        // until the core renderer is proven stable on-device.
        initialize(new NewtonsCradleGame(this), cfg);
    }

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
