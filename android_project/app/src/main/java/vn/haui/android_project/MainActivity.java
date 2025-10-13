package vn.haui.android_project;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import vn.haui.android_project.ui.main.SplashCreen.SplashScreen;

public class MainActivity extends AppCompatActivity {
    private FrameLayout container;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.containerMain);
    }

    public void showScreen(Object screen) {
        View view = null;
        if (screen instanceof SplashScreen)
            view = ((SplashScreen) screen).getView();
//        else if (screen instanceof LoginScreen)
//            view = ((LoginScreen) screen).getView();
//        else if (screen instanceof HomeScreen)
//            view = ((HomeScreen) screen).getView();

        if (view != null) {
            container.removeAllViews();
            container.addView(view);
        }
    }
}