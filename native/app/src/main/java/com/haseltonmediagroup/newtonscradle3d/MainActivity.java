package com.haseltonmediagroup.newtonscradle3d;

import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class MainActivity extends AndroidApplication implements NewtonsCradleGame.PlatformBridge {
    private static final String BANNER_ID = "ca-app-pub-1051867648799965/1603031670";
    private static final String INTERSTITIAL_ID = "ca-app-pub-1051867648799965/7015140451";

    private Vibrator vibrator;
    private long lastImpactMs;
    private AdView banner;
    private InterstitialAd interstitial;
    private int naturalBreaks;
    private boolean adsStarted;

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
        root.setBackgroundColor(Color.rgb(5, 7, 11));

        FrameLayout.LayoutParams gameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        gameParams.bottomMargin = dp(56);
        root.addView(gameView, gameParams);

        TextView title = new TextView(this);
        title.setText("NEWTON'S CRADLE");
        title.setTextColor(Color.rgb(225, 230, 238));
        title.setTextSize(18f);
        title.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(0.12f);
        title.setShadowLayer(10f, 0f, 2f, Color.BLACK);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52), Gravity.TOP);
        titleParams.topMargin = dp(10);
        root.addView(title, titleParams);

        FrameLayout adSlot = new FrameLayout(this);
        adSlot.setBackgroundColor(Color.rgb(9, 11, 15));
        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56), Gravity.BOTTOM);
        root.addView(adSlot, adParams);
        setContentView(root);

        requestConsentThenStartAds(adSlot);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void requestConsentThenStartAds(FrameLayout adSlot) {
        ConsentInformation consent = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consent.requestConsentInfoUpdate(this, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        this, formError -> {
                            if (consent.canRequestAds()) startAds(adSlot);
                        }),
                requestError -> {
                    if (consent.canRequestAds()) startAds(adSlot);
                });
        if (consent.canRequestAds()) startAds(adSlot);
    }

    private synchronized void startAds(FrameLayout adSlot) {
        if (adsStarted) return;
        adsStarted = true;
        MobileAds.initialize(this, status -> runOnUiThread(() -> {
            banner = new AdView(this);
            banner.setAdUnitId(BANNER_ID);
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int widthDp = Math.max(320, Math.round(metrics.widthPixels / metrics.density));
            banner.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, widthDp));
            adSlot.removeAllViews();
            adSlot.addView(banner, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            banner.loadAd(new AdRequest.Builder().build());
            loadInterstitial();
        }));
    }

    private void loadInterstitial() {
        InterstitialAd.load(this, INTERSTITIAL_ID, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitial = ad;
                        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override public void onAdDismissedFullScreenContent() {
                                interstitial = null;
                                loadInterstitial();
                            }
                        });
                    }
                    @Override public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        interstitial = null;
                    }
                });
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

    @Override
    public void naturalBreak() {
        naturalBreaks++;
        if (naturalBreaks % 6 == 0) {
            runOnUiThread(() -> {
                if (interstitial != null) interstitial.show(this);
            });
        }
    }

    @Override protected void onPause() {
        if (banner != null) banner.pause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (banner != null) banner.resume();
    }

    @Override protected void onDestroy() {
        if (banner != null) banner.destroy();
        super.onDestroy();
    }
}
