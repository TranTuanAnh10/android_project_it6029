package vn.haui.android_project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.haui.android_project.entity.DeviceToken;
import vn.haui.android_project.entity.UserEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.view.HomeFragment;
import vn.haui.android_project.view.NotificationsFragment;
import vn.haui.android_project.view.OrdersFragment;
import vn.haui.android_project.view.ProfileFragment;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    String userId, userEmail, userName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();

        if (intent != null) {
            userId = intent.getStringExtra("USER_ID");
            userEmail = intent.getStringExtra("USER_EMAIL");
            userName = intent.getStringExtra("USER_NAME");
        }

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        saveUserFcmToken();
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new HomeFragment())
                .commit();

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
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }


    private void saveUserFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    String deviceModel = android.os.Build.MODEL;
                    String osVersion = android.os.Build.VERSION.RELEASE;

                    DeviceToken newToken = new DeviceToken(
                            token,
                            deviceModel,
                            osVersion,
                            System.currentTimeMillis()
                    );
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    DocumentReference userRef = db.collection(DatabaseTable.USERS.getValue()).document(userId);
                    userRef.get().addOnSuccessListener(snapshot -> {
                        if (!snapshot.exists()) return;
                        UserEntity user = snapshot.toObject(UserEntity.class);
                        if (user == null) return;
                        List<DeviceToken> tokens = user.getTokens() != null ? user.getTokens() : new ArrayList<>();
                        boolean exists = false;
                        for (DeviceToken t : tokens) {
                            if (t.getToken().equals(token)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            // ✅ Thêm token mới bằng cách append vào mảng hiện có
                            userRef.update("tokens", FieldValue.arrayUnion(newToken))
                                    .addOnSuccessListener(a -> Log.d("FCM", "Đã thêm token mới cho user"))
                                    .addOnFailureListener(e -> Log.e("FCM", "Lỗi khi thêm token", e));
                        } else {
                            Log.d("FCM", "Token đã tồn tại, không cần thêm");
                        }
                    }).addOnFailureListener(e -> Log.e("FCM", "Lỗi lấy user", e));
                })
                .addOnFailureListener(e -> Log.e("FCM", "Không lấy được token FCM", e));
    }

}