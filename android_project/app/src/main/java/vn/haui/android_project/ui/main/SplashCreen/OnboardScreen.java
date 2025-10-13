package vn.haui.android_project.ui.main.SplashCreen;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;

public class OnboardScreen {
    private final Context context;
    private final View rootView;

    public OnboardScreen(Context context) {
        this.context = context;
        this.rootView = LayoutInflater.from(context).inflate(R.layout.splash_screen, null);
        start();
    }
    private void start() {
        
    }
}
