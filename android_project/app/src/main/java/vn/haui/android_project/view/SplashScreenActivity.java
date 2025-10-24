package vn.haui.android_project.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;
import vn.haui.android_project.services.FirebaseUserManager;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                if (currentUser != null) {
                    gotoMain(currentUser);
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


    private void gotoMain(@NonNull FirebaseUser user) {
        FirebaseUserManager userManager = new FirebaseUserManager();
        userManager.getUserByUid(user.getUid(), userData -> {
            // Lấy dữ liệu từ Firestore hoặc từ Auth nếu Firestore không có
            String phone = (String) userData.getOrDefault("phoneNumber", "");
            if (phone.isBlank()){
                // chua co std thi day den nhap std
                Intent intent = new Intent(SplashScreenActivity.this, PhoneScreenActivity.class);
                intent.putExtra("CURRENT_USER", user);
                startActivity(intent);
                finish();
            }else {
                // da co std thi day den main
                Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                intent.putExtra("USER_ID", user.getUid());
                intent.putExtra("USER_EMAIL", user.getEmail());
                intent.putExtra("USER_NAME", user.getDisplayName());
                if (user.getPhotoUrl() != null)
                    intent.putExtra("USER_PHOTO", user.getPhotoUrl().toString());
                startActivity(intent);
                finish();
            }
        }, error -> {
            Toast.makeText(this, "Không tìm thấy thông tin tài khoản", Toast.LENGTH_SHORT).show();
        });
    }
}