package vn.haui.android_project.ui.main.SplashCreen;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;

public class SplashScreen {
    private final Context context;
    private final View rootView;

    public SplashScreen(Context context) {
        this.context = context;
        this.rootView = LayoutInflater.from(context).inflate(R.layout.splash_screen, null);
        start();
    }

    private void start() {
        new Handler().postDelayed(() -> {
//            SessionManager session = new SessionManager(context);
//            if (session.isLoggedIn()) {
//                ((MainActivity) context).showScreen(new HomeScreen(context));
//            } else {
//                ((MainActivity) context).showScreen(new LoginScreen(context));
//            }
            ((MainActivity) context).showScreen(new OnboardScreen(context));
        }, 2000);
    }

    public View getView() {
        return rootView;
    }
}