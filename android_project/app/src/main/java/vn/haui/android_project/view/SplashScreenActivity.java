package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;

public class SplashScreenActivity extends AppCompatActivity {

    private LinearLayout onBoardingPanel, splashPanel;
    private ImageButton btnNext;
    private static final int SPLASH_TIME_OUT = 2000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.splash_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mapping();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                if (currentUser != null) {
                    String uid = currentUser.getUid();
                    String userEmail = currentUser.getEmail();
                    String displayName = currentUser.getDisplayName();
                    Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                    intent.putExtra("USER_ID", uid);
                    intent.putExtra("USER_EMAIL", userEmail);
                    intent.putExtra("USER_NAME", displayName);
                    startActivity(intent);
                }
                else {
                    splashPanel.setVisibility(View.GONE);
                    onBoardingPanel.setVisibility(View.VISIBLE);
                }
            }
        }, SPLASH_TIME_OUT);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SplashScreenActivity.this, LoginScreenActivity.class);
                startActivity(intent);
            }
        });
    }

    private void mapping(){
        splashPanel = findViewById(R.id.splash_panel);
        onBoardingPanel = findViewById(R.id.on_boarding_panel);
        btnNext = findViewById(R.id.btnNext);
    }
}