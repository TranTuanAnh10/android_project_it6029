package vn.haui.android_project.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.enums.UserRole;

public class SplashScreenActivity extends AppCompatActivity {

    private static final String TAG = "SplashScreenActivity";
    private LinearLayout onBoardingPanel, splashPanel;
    private ImageButton btnNext;
    private static final int SPLASH_TIME_OUT = 500;

    // 1. Định nghĩa launcher để xử lý kết quả yêu cầu nhiều quyền
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    // 2. Định nghĩa các quyền cơ bản cần thiết
    private final String[] BASE_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION, // Địa điểm hiện tại
            Manifest.permission.SEND_SMS,             // SMS
            Manifest.permission.CALL_PHONE            // Gọi điện
    };

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

        // 3. Khởi tạo ActivityResultLauncher
        setupPermissionsLauncher();

        // 4. Bắt đầu kiểm tra quyền
        checkPermissionsAndProceed();

        // Listener cho nút OnBoarding (giữ nguyên)
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SplashScreenActivity.this, LoginScreenActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupPermissionsLauncher() {
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::handlePermissionResults
        );
    }

    private String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>(Arrays.asList(BASE_PERMISSIONS));
        // Thêm quyền thông báo chỉ cho Android 13 (API 33) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return permissions.toArray(new String[0]);
    }

    private void checkPermissionsAndProceed() {
        String[] requiredPermissions = getRequiredPermissions();
        List<String> permissionsToRequest = new ArrayList<>();
        boolean allGranted = true;

        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                permissionsToRequest.add(permission);
            }
        }

        if (allGranted) {
            // Đã cấp đầy đủ quyền -> Chuyển sang màn hình tiếp theo
            Log.d(TAG, "Tất cả các quyền đã được cấp.");
            proceedToNextScreen();
        } else {
            // Yêu cầu các quyền còn thiếu
            Log.d(TAG, "Yêu cầu cấp các quyền còn thiếu: " + permissionsToRequest.toString());
            requestPermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

    private void handlePermissionResults(Map<String, Boolean> permissions) {
        boolean allGranted = true;
        for (Boolean granted : permissions.values()) {
            if (!granted) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            Toast.makeText(this, "Đã cấp đầy đủ quyền truy cập. Khởi động ứng dụng.", Toast.LENGTH_SHORT).show();
        } else {
            // Cảnh báo người dùng về việc thiếu quyền
            Toast.makeText(this, "Một số quyền quan trọng chưa được cấp. Một số chức năng sẽ bị giới hạn.", Toast.LENGTH_LONG).show();
        }

        // Dù có quyền hay không, vẫn tiến hành chuyển màn hình sau khi xử lý xong quyền
        proceedToNextScreen();
    }

    private void mapping(){
        splashPanel = findViewById(R.id.splash_panel);
        onBoardingPanel = findViewById(R.id.on_boarding_panel);
        btnNext = findViewById(R.id.btnNext);
    }

    /**
     * Logic chuyển màn hình sau khi hoàn tất kiểm tra/yêu cầu quyền.
     * Đây là đoạn code cũ của bạn, đã được chuyển vào hàm này.
     */
    private void proceedToNextScreen() {
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
                        intent.putExtra("USER_NAME", userFirebase.getName());
                        startActivity(intent);
                        finish();
                    } else if (role != null && role.equals(UserRole.ADMIN.getValue())) {
                        Toast.makeText(SplashScreenActivity.this, "Đang tải dữ liệu", Toast.LENGTH_SHORT).show();
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