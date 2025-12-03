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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.enums.UserRole;
import vn.haui.android_project.services.FirebaseUserManager;

public class SplashScreenActivity extends AppCompatActivity {

    private LinearLayout onBoardingPanel, splashPanel;
    private ImageButton btnNext;
    private static final int SPLASH_TIME_OUT = 500;
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection(DatabaseTable.USERS.getValue()).document(user.getUid());
        userRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        Intent intent = new Intent(SplashScreenActivity.this, PhoneScreenActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    UserEntity userFirebase = snapshot.toObject(UserEntity.class);

                    if (userFirebase == null) {
                        loadMainActivity(user);
                        return;
                    }

                    String phone = userFirebase.getPhoneNumber();
                    if (phone == null || phone.isBlank()) {
                        Intent intent = new Intent(SplashScreenActivity.this, PhoneScreenActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    String role = userFirebase.getRole();

                    if (role != null && role.equals(UserRole.SHIPPER.getValue())) {
                        Intent intent = new Intent(SplashScreenActivity.this, ShipperActivity.class);
                        intent.putExtra("USER_PHONE", phone);
                        intent.putExtra("USER_EMAIL", user.getEmail());
                        intent.putExtra("USER_NAME", user.getDisplayName());
                        startActivity(intent);
                        finish();
                    } else if (role != null && role.equals(UserRole.ADMIN.getValue())) {
                        Toast.makeText(SplashScreenActivity.this, "Comming soon", Toast.LENGTH_SHORT).show();
                        loadMainActivity(user);
                    } else {
                        loadMainActivity(user);
                    }
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(SplashScreenActivity.this, "Không tìm thấy thông tin tài khoản", Toast.LENGTH_SHORT).show();
                     Intent intent = new Intent(SplashScreenActivity.this, LoginScreenActivity.class);
                     startActivity(intent);
                     finish();
                });
    }
    private void loadMainActivity(FirebaseUser user){
        Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
        intent.putExtra("USER_ID", user.getUid());
        intent.putExtra("USER_EMAIL", user.getEmail());
        intent.putExtra("USER_NAME", user.getDisplayName());
        if (user.getPhotoUrl() != null)
            intent.putExtra("USER_PHOTO", user.getPhotoUrl().toString());
        startActivity(intent);
        finish();
    }
}