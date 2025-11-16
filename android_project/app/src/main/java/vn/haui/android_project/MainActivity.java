// D:/.../app/src/main/java/vn/haui/android_project/MainActivity.java
package vn.haui.android_project;

// Các import...
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull; // <<--- BẮT BUỘC THÊM
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.ArrayList;
import java.util.List;
import vn.haui.android_project.entity.DeviceToken;
import vn.haui.android_project.entity.UserEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.services.FirebaseUserManager;
import vn.haui.android_project.view.AdminScreenActivity; // <<--- SỬA LẠI ĐƯỜNG DẪN
import vn.haui.android_project.view.EmployeeScreenActivity; // <<--- SỬA LẠI ĐƯỜNG DẪN
import vn.haui.android_project.view.HomeFragment;
import vn.haui.android_project.view.LoginScreenActivity;
import vn.haui.android_project.view.NotificationsFragment;
import vn.haui.android_project.view.OrdersFragment;
import vn.haui.android_project.view.ProfileFragment;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        container = findViewById(R.id.container);

        // Ẩn giao diện đi trong khi kiểm tra quyền
        bottomNavigationView.setVisibility(View.GONE);
        container.setVisibility(View.GONE);

        checkUserRoleAndRedirect();
    }

    private void checkUserRoleAndRedirect() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        FirebaseUserManager userManager = new FirebaseUserManager();
        userManager.getUserByUid(currentUser.getUid(), userData -> {
            String role = (String) userData.getOrDefault("role", "customer");
            switch (role.toLowerCase()) {
                case "admin":
                    startActivity(new Intent(MainActivity.this, AdminScreenActivity.class));
                    finish();
                    break;
                case "employee":
                    startActivity(new Intent(MainActivity.this, EmployeeScreenActivity.class));
                    finish();
                    break;
                case "customer":
                default:
                    // SỬA Ở ĐÂY: Truyền currentUser vào
                    setupCustomerUI(currentUser);
                    break;
            }
        }, error -> {
            Toast.makeText(this, "Lỗi xác thực, vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            goToLogin();
        });
    }

    private void goToLogin() {
        startActivity(new Intent(MainActivity.this, LoginScreenActivity.class));
        finish();
    }

    // SỬA Ở ĐÂY: Hàm này giờ nhận vào một FirebaseUser
    private void setupCustomerUI(@NonNull FirebaseUser user) {
        bottomNavigationView.setVisibility(View.VISIBLE);
        container.setVisibility(View.VISIBLE);

        // SỬA Ở ĐÂY: Lấy userId từ user và truyền vào hàm saveUserFcmToken
        String userId = user.getUid();
        saveUserFcmToken(userId);

        getSupportFragmentManager().beginTransaction().replace(R.id.container, new HomeFragment()).commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.btn_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.btn_order) {
                selectedFragment = new OrdersFragment();
            } else if (itemId == R.id.btn_notification) {
                selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.btn_profile) {
                selectedFragment = new ProfileFragment();
            }
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, selectedFragment).commit();
            }
            return true;
        });
    }

    // SỬA Ở ĐÂY: Hàm này giờ nhận vào một chuỗi userId
    private void saveUserFcmToken(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e("FCM", "userId bị null, không thể lưu token.");
            return;
        }

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            String deviceModel = android.os.Build.MODEL;
            String osVersion = android.os.Build.VERSION.RELEASE;
            DeviceToken newToken = new DeviceToken(token, deviceModel, osVersion, System.currentTimeMillis());
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Dòng này giờ sẽ an toàn vì userId không bao giờ null
            DocumentReference userRef = db.collection(DatabaseTable.USERS.getValue()).document(userId);

            userRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    UserEntity userEntity = snapshot.toObject(UserEntity.class);
                    if (userEntity != null) {
                        List<DeviceToken> tokens = userEntity.getTokens() != null ? userEntity.getTokens() : new ArrayList<>();
                        boolean exists = tokens.stream().anyMatch(t -> t.getToken().equals(token));
                        if (!exists) {
                            userRef.update("tokens", FieldValue.arrayUnion(newToken));
                        }
                    }
                }
            });
        });
    }
}
