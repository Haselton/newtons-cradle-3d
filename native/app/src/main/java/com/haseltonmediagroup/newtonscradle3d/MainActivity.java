package com.haseltonmediagroup.newtonscradle3d;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;

public class MainActivity extends Activity {
    private CradleView cradleView;
    private SoundPool soundPool;
    private int impactSoundId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attributes)
                .build();
        impactSoundId = soundPool.load(this, R.raw.newton_impact, 1);

        cradleView = new CradleView(this, strength -> {
            if (soundPool == null || impactSoundId == 0) return;
            float volume = 0.28f + 0.72f * strength;
            float rate = 0.97f + 0.06f * strength;
            soundPool.play(impactSoundId, volume, volume, 1, 0, rate);
        });
        setContentView(cradleView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cradleView != null) cradleView.resume();
    }

    @Override
    protected void onPause() {
        if (cradleView != null) cradleView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        super.onDestroy();
    }
}
