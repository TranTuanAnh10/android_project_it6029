// D:/.../app/src/main/java/vn/haui/android_project/view/AdminScreenActivity.java
package vn.haui.android_project.view;

import android.os.Bundle;
import android.util.Log;

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

import vn.haui.android_project.R;
import vn.haui.android_project.entity.DeviceToken;
import vn.haui.android_project.entity.UserEntity;
import vn.haui.android_project.enums.DatabaseTable;

public class AdminScreenActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_screen);

        // --- BƯỚC 1: LẤY THÔNG TIN USER VÀ LƯU TOKEN (Phần này đã đúng) ---
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            saveUserFcmToken(currentUser.getUid());
        } else {
            Log.e("AdminScreen", "Lỗi nghiêm trọng: Không có admin nào đăng nhập.");
        }

        // --- BƯỚC 2: PHỤC HỒI GIAO DIỆN ĐÚNG CHO ADMIN ---
        bottomNavigationView = findViewById(R.id.admin_bottom_navigation);

        // Thiết lập Fragment mặc định cho admin
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.admin_container, new ReportFragment())
                    .commit();
        }

        // Thiết lập listener cho Bottom Navigation của admin
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // Sử dụng đúng ID và đúng Fragment của admin
            if (itemId == R.id.btn_order) {
                selectedFragment = new OrderManagementFragment();
            } else if (itemId == R.id.btn_product) {
                selectedFragment = new ProductManagementFragment();
            } else if (itemId == R.id.btn_employee) {
                selectedFragment = new UserManagementFragment();
            }else if (itemId == R.id.btn_profile) {
                selectedFragment = new ProfileFragment();
            }else if (itemId == R.id.btn_report) {
                selectedFragment = new ReportFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.admin_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }

    // Hàm saveUserFcmToken giữ nguyên, không cần sửa đổi gì thêm
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
