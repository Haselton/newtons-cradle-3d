package com.haseltonmediagroup.newtonscradle3d;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public class MainActivity extends Activity {
    private CradleView cradleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cradleView = new CradleView(this);
        setContentView(cradleView);

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
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
