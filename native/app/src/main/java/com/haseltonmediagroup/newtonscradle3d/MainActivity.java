package com.haseltonmediagroup.newtonscradle3d;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public class MainActivity extends Activity {
    private CradleView cradleView;
    private Vibrator vibrator;
    private short[] impactPcm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vibrator = getVibrator();
        impactPcm = makeMetalImpact();

        cradleView = new CradleView(this, strength -> runOnUiThread(() -> {
            vibrateImpact(strength);
            playImpact(strength);
        }));
        setContentView(cradleView);
    }

    private Vibrator getVibrator() {
        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm != null ? vm.getDefaultVibrator() : null;
        }
        return (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void vibrateImpact(float strength) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long duration = strength > .55f ? 24L : 14L;
        int amplitude = Math.min(255, 90 + (int)(strength * 150f));
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude));
        } else {
            vibrator.vibrate(duration);
        }
    }

    private short[] makeMetalImpact() {
        final int sr = 22050;
        final int count = (int)(sr * 0.24f);
        short[] data = new short[count];
        long seed = 0x5EED1234L;
        for (int i = 0; i < count; i++) {
            double t = i / (double) sr;
            double env = Math.exp(-21.0 * t);
            double ring = 0.58 * Math.sin(2.0 * Math.PI * 1680.0 * t)
                    + 0.30 * Math.sin(2.0 * Math.PI * 2470.0 * t + .35)
                    + 0.18 * Math.sin(2.0 * Math.PI * 3860.0 * t + .9)
                    + 0.10 * Math.sin(2.0 * Math.PI * 5100.0 * t + .2);
            seed = seed * 1664525L + 1013904223L;
            double noise = (((seed >>> 16) & 0xffff) / 32768.0 - 1.0) * Math.exp(-70.0 * t) * .22;
            double click = ring * env + noise;
            data[i] = (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, click * 21000.0));
        }
        return data;
    }

    private void playImpact(float strength) {
        final short[] pcm = impactPcm;
        if (pcm == null) return;
        new Thread(() -> {
            try {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                AudioFormat format = new AudioFormat.Builder()
                        .setSampleRate(22050)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build();
                AudioTrack track = new AudioTrack.Builder()
                        .setAudioAttributes(attrs)
                        .setAudioFormat(format)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(pcm.length * 2)
                        .build();
                track.write(pcm, 0, pcm.length);
                float volume = .35f + .65f * strength;
                track.setVolume(volume);
                track.play();
                Thread.sleep(300);
                track.stop();
                track.release();
            } catch (Throwable ignored) { }
        }, "impact-audio").start();
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
}
