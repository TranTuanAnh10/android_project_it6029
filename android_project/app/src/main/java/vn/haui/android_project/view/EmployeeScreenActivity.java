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

public class EmployeeScreenActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Thay vì dùng EdgeToEdge, ta sẽ dùng layout đơn giản hơn
        setContentView(R.layout.activity_employee_screen);

        // --- BƯỚC 1: LẤY THÔNG TIN USER VÀ LƯU TOKEN ---
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            saveUserFcmToken(currentUser.getUid());
        } else {
            Log.e("EmployeeScreen", "Lỗi nghiêm trọng: Không có nhân viên nào đăng nhập.");
        }

        // --- BƯỚC 2: THIẾT LẬP GIAO DIỆN CHO NHÂN VIÊN ---
        bottomNavigationView = findViewById(R.id.employee_bottom_navigation);

        // Thiết lập Fragment mặc định cho nhân viên
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.employee_container, new ReportFragment())
                    .commit();
        }
        // Trong onCreate của màn hình Employee
        FirebaseMessaging.getInstance().subscribeToTopic("orders")
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Đăng ký thất bại", task.getException());
                    }
                });


        // Thiết lập listener cho Bottom Navigation của nhân viên
        // Hiện tại chỉ có 1 tab nên không cần listener, nhưng ta cứ để sẵn để dễ mở rộng
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // Chỉ có một lựa chọn là quản lý đơn hàng
            if (itemId == R.id.btn_order) {
                selectedFragment = new OrderManagementFragment();
            }
            if (itemId == R.id.btn_product) {
                selectedFragment = new ProductManagementFragment();
            }
            if (itemId == R.id.btn_profile) {
                selectedFragment = new ProfileFragment();
            }
            if (itemId == R.id.btn_report) {
                selectedFragment = new ReportFragment();
            }
            // Bạn có thể thêm các case else if khác ở đây nếu nhân viên có thêm chức năng

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.employee_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Đảm bảo tab đầu tiên được chọn
        bottomNavigationView.setSelectedItemId(R.id.btn_order);
    }

    // Hàm này dùng để lưu token, tương tự như của Admin
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
                            userRef.update("tokens", FieldValue.arrayUnion(newToken))
                                    .addOnSuccessListener(a -> Log.d("FCM", "Đã thêm token mới cho employee"))
                                    .addOnFailureListener(e -> Log.e("FCM", "Lỗi khi thêm token", e));
                        }
                    }
                }
            });
        });
    }
}
