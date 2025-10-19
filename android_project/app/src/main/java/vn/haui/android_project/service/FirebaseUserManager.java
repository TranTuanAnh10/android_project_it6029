package vn.haui.android_project.service;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import vn.haui.android_project.entity.UserEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.enums.UserRole;

public class FirebaseUserManager {
    private static final String TAG = "FirebaseUserManager";
    private final FirebaseFirestore db;

    public FirebaseUserManager() {
        db = FirebaseFirestore.getInstance();
    }

    // ✅ Lưu hoặc cập nhật người dùng sau khi đăng nhập
    public void saveOrUpdateUser(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection(DatabaseTable.USERS.getValue()).document(firebaseUser.getUid());

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                // 🔰 Lần đầu đăng nhập → tạo user mới
                UserEntity newUser = new UserEntity(
                        firebaseUser.getUid(),
                        firebaseUser.getDisplayName(),
                        firebaseUser.getEmail(),
                        firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "",
                        UserRole.USER.getValue(),
                        getCurrentTime(),
                        getCurrentTime()
                );

                userRef.set(newUser)
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "✅ Đã tạo người dùng mới"))
                        .addOnFailureListener(e -> Log.e("Firestore", "❌ Lỗi khi tạo người dùng", e));

            } else {
                // 🔁 Đã tồn tại → chỉ cập nhật thời gian đăng nhập
                userRef.update("lastLogin", getCurrentTime())
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "✅ Đã cập nhật lastLogin"))
                        .addOnFailureListener(e -> Log.e("Firestore", "❌ Lỗi khi cập nhật lastLogin", e));
            }
        }).addOnFailureListener(e -> Log.e("Firestore", "❌ Không thể kiểm tra người dùng", e));
    }

    public void getUserByUid(String uid, Consumer<Map<String, Object>> onSuccess, @Nullable Consumer<Exception> onError) {
        if (uid == null || uid.isEmpty()) {
            if (onError != null) onError.accept(new IllegalArgumentException("UID rỗng"));
            return;
        }

        DocumentReference userRef = db.collection(DatabaseTable.USERS.getValue()).document(uid);
        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> userData = documentSnapshot.getData();
                        onSuccess.accept(userData);
                    } else {
                        if (onError != null) onError.accept(new Exception("User không tồn tại"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi lấy user", e);
                    if (onError != null) onError.accept(e);
                });
    }
    // ✅ Hàm cập nhật avatarUrl (khi người dùng thay đổi ảnh)
    public void updateAvatar(String uid, String newAvatarUrl) {
        if (uid == null || newAvatarUrl == null) return;

        db.collection(DatabaseTable.USERS.getValue()).document(uid)
                .update("avatarUrl", newAvatarUrl, "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Cập nhật avatar thành công"))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi cập nhật avatar", e));
    }

    // ✅ Hàm cập nhật tên, email,... nếu cần
    public void updateUserInfo(String uid, Map<String, Object> updates) {
        if (uid == null || updates == null) return;

        updates.put("updatedAt", FieldValue.serverTimestamp());
        db.collection(DatabaseTable.USERS.getValue()).document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Cập nhật thông tin user thành công"))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi cập nhật user", e));
    }


    private String getCurrentTime() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return LocalDateTime.now().toString();
        }
        return "";
    }
}
