package vn.haui.android_project.service;

import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import vn.haui.android_project.entity.UserEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.enums.UserRole;

public class FirebaseUserManager {
    private static final String TAG = "FirebaseUserManager";
    private final FirebaseFirestore db;
    private final FirebaseStorage storage; // Thêm Firebase Storage
    public FirebaseUserManager() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
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
                        getCurrentTime(),
                        firebaseUser.getPhoneNumber() != null ? firebaseUser.getPhoneNumber() : ""
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
    public void uploadAvatar(String uid, Uri imageUri, BiConsumer<Boolean, String> onComplete) {
        if (uid == null || imageUri == null) {
            onComplete.accept(false, "UID hoặc Image URI không hợp lệ.");
            return;
        }

        // Tạo đường dẫn trên Firebase Storage: avatars/{uid}/{tên file}
        StorageReference storageRef = storage.getReference()
                .child("avatars")
                .child(uid)
                .child("profile.jpg"); // Có thể dùng UUID.randomUUID().toString() để tên file là duy nhất

        // Bắt đầu tải file lên
        UploadTask uploadTask = storageRef.putFile(imageUri);

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            // Lấy URL tải xuống sau khi upload thành công
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                Log.d(TAG, "✅ Tải ảnh lên thành công, URL: " + downloadUri.toString());
                onComplete.accept(true, downloadUri.toString());
            } else {
                Log.e(TAG, "❌ Lỗi khi tải ảnh lên", task.getException());
                onComplete.accept(false, "Lỗi: " + task.getException().getMessage());
            }
        });
    }

    // --- HÀM CẬP NHẬT USERINFO MỚI ---
    /**
     * Cập nhật thông tin người dùng vào Firestore.
     * @param uid UID của người dùng.
     * @param updates Map chứa các trường cần cập nhật.
     * @param onSuccess Callback khi thành công.
     * @param onError Callback khi thất bại.
     */
    public void updateUser(String uid, Map<String, Object> updates, Consumer<Void> onSuccess, @Nullable Consumer<Exception> onError) {
        if (uid == null || updates == null || updates.isEmpty()) {
            if (onError != null) onError.accept(new IllegalArgumentException("Dữ liệu cập nhật không hợp lệ."));
            return;
        }

        updates.put("updatedAt", FieldValue.serverTimestamp()); // Luôn cập nhật thời gian
        db.collection(DatabaseTable.USERS.getValue()).document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Cập nhật thông tin user thành công");
                    if (onSuccess != null) onSuccess.accept(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi cập nhật user", e);
                    if (onError != null) onError.accept(e);
                });
    }


    private String getCurrentTime() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return LocalDateTime.now().toString();
        }
        return "";
    }
}
