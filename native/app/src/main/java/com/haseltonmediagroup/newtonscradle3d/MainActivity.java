package com.haseltonmediagroup.newtonscradle3d;

import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class MainActivity extends AndroidApplication implements NewtonsCradleGame.PlatformBridge {
    private Vibrator vibrator;
    private long lastImpactMs;
    private AdView adView;
    private FrameLayout adContainer;
    private Button privacyButton;
    private ConsentInformation consentInformation;
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

        adContainer = new FrameLayout(this);
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

        privacyButton = new Button(this);
        privacyButton.setText("Privacy choices");
        privacyButton.setTextSize(10);
        privacyButton.setTextColor(Color.WHITE);
        privacyButton.setBackgroundColor(Color.argb(170, 18, 19, 22));
        privacyButton.setVisibility(View.GONE);
        privacyButton.setOnClickListener(v -> UserMessagingPlatform.showPrivacyOptionsForm(
                this, formError -> updatePrivacyButton()));
        FrameLayout.LayoutParams privacyParams = new FrameLayout.LayoutParams(
                dp(116), dp(38), Gravity.BOTTOM | Gravity.END);
        privacyParams.bottomMargin = dp(52);
        privacyParams.rightMargin = dp(8);
        root.addView(privacyButton, privacyParams);

        setContentView(root);
        gatherConsentAndLoadAds();
    }

    private void gatherConsentAndLoadAds() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> {
                    updatePrivacyButton();
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(this, formError -> {
                        updatePrivacyButton();
                        if (consentInformation.canRequestAds()) startAds();
                    });
                    if (consentInformation.canRequestAds()) startAds();
                },
                requestConsentError -> {
                    updatePrivacyButton();
                    if (consentInformation.canRequestAds()) startAds();
                });
    }

    private void updatePrivacyButton() {
        runOnUiThread(() -> privacyButton.setVisibility(
                consentInformation != null &&
                consentInformation.getPrivacyOptionsRequirementStatus()
                        == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                        ? View.VISIBLE : View.GONE));
    }

    private synchronized void startAds() {
        if (adsStarted) return;
        adsStarted = true;
        MobileAds.initialize(this, status -> runOnUiThread(this::loadBanner));
    }

    private void loadBanner() {
        adView = new AdView(this);
        adView.setAdUnitId(BuildConfig.ADMOB_BANNER_ID);
        int widthPx = getResources().getDisplayMetrics().widthPixels;
        int widthDp = Math.max(320, Math.round(widthPx / getResources().getDisplayMetrics().density));
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, widthDp));
        adContainer.removeAllViews();
        adContainer.addView(adView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        adView.loadAd(new AdRequest.Builder().build());
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

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }
}
