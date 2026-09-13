package com.haseltonmediagroup.newtonscradle3d;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    private CradleView cradleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cradleView = new CradleView(this);
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
}
