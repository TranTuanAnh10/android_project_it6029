package vn.haui.android_project.services;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.enums.DatabaseTable;

public class FirebaseLocationManager {
    private static final String TAG = "FirebaseLocationManager";
    private final FirebaseFirestore db;

    public FirebaseLocationManager() {
        db = FirebaseFirestore.getInstance();
    }


    public void appendLocation(String uid, UserLocationEntity newLocation, BiConsumer<Boolean, String> onComplete) {
        if (uid == null || uid.isEmpty() || newLocation == null) {
            onComplete.accept(false, "UID hoặc Location rỗng");
            return;
        }

        DocumentReference userDocRef = db.collection(DatabaseTable.USER_LOCATIONS.getValue()).document(uid);

        // Gán ID duy nhất cho location mới
        newLocation.setId(String.valueOf(System.currentTimeMillis()));

        userDocRef.update("locations", FieldValue.arrayUnion(newLocation))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Thêm location mới vào danh sách của UID: " + uid);
                    onComplete.accept(true, uid);
                })
                .addOnFailureListener(e -> {
                    // Nếu field "locations" chưa tồn tại, cần tạo mới document
                    userDocRef.set(new HashMap<String, Object>() {{
                                put("locations", List.of(newLocation));
                            }})
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Tạo document mới và thêm location đầu tiên cho UID: " + uid);
                                onComplete.accept(true, uid);
                            })
                            .addOnFailureListener(ex -> {
                                Log.e(TAG, "❌ Lỗi thêm location: " + ex.getMessage());
                                onComplete.accept(false, ex.getMessage());
                            });
                });
    }



    /**
     * 🔄 Cập nhật toạ độ của người dùng (ví dụ khi người dùng di chuyển).
     * Ghi đè trực tiếp các trường trong Document UID.
     */
    public void updateLocation(String uid, Map<String, Object> updates,
                               Consumer<Void> onSuccess, @Nullable Consumer<Exception> onError) {
        if (uid == null || uid.isEmpty()) {
            if (onError != null) onError.accept(new IllegalArgumentException("UID rỗng"));
            return;
        }

        // --- SỬA ĐỔI: TRUY CẬP TRỰC TIẾP document(uid) ---
        db.collection(DatabaseTable.USER_LOCATIONS.getValue()).document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Cập nhật toạ độ thành công cho UID: " + uid);
                    if (onSuccess != null) onSuccess.accept(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi khi cập nhật toạ độ cho UID: " + uid + " : " + e.getMessage());
                    if (onError != null) onError.accept(e);
                });
    }


    /**
     * 📍 Lấy dữ liệu toạ độ cuối cùng của người dùng (lấy toàn bộ document UID).
     */
    public void getLocationByUid(String uid, Consumer<DocumentSnapshot> onSuccess,
                                 @Nullable Consumer<Exception> onError) {
        if (uid == null || uid.isEmpty()) {
            if (onError != null) onError.accept(new IllegalArgumentException("UID rỗng"));
            return;
        }

        // --- SỬA ĐỔI: TRUY CẬP TRỰC TIẾP document(uid) và dùng .get() ---
        db.collection(DatabaseTable.USER_LOCATIONS.getValue()).document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Trả về toàn bộ DocumentSnapshot, bao gồm cả ID và dữ liệu
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "✅ Lấy toạ độ thành công cho UID: " + uid);
                    } else {
                        Log.d(TAG, "🔍 Document UID không tồn tại: " + uid);
                    }
                    onSuccess.accept(documentSnapshot);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi lấy toạ độ cho UID: " + uid + " : " + e.getMessage());
                    if (onError != null) onError.accept(e);
                });
    }
}
