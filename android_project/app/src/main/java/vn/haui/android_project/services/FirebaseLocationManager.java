package vn.haui.android_project.services;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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


    public void getSortedLocationsByUid(String uid, BiConsumer<Boolean, List<UserLocationEntity>> onComplete) {
        if (uid == null || uid.isEmpty()) {
            onComplete.accept(false, Collections.emptyList());
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(DatabaseTable.USER_LOCATIONS.getValue()).document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        onComplete.accept(true, Collections.emptyList());
                        return;
                    }

                    Object locationsObject = documentSnapshot.get("locations");
                    if (!(locationsObject instanceof List)) {
                        onComplete.accept(true, Collections.emptyList());
                        return;
                    }

                    List<UserLocationEntity> locationList = new ArrayList<>();
                    List<Map<String, Object>> rawLocations = (List<Map<String, Object>>) locationsObject;

                    for (Map<String, Object> locationMap : rawLocations) {
                        try {
                            // --- SỬA LỖI: Ánh xạ thủ công (Manual Mapping) ---
                            UserLocationEntity location = mapToUserLocationEntity(locationMap);
                            locationList.add(location);

                        } catch (Exception e) {
                            Log.e(TAG, "❌ Lỗi ánh xạ đối tượng: " + e.getMessage());
                        }
                    }

                    // Sắp xếp danh sách (Client-side Sorting)
                    locationList.sort(Comparator.comparing(UserLocationEntity::isDefaultLocation).reversed());

                    Log.d(TAG, "✅ Lấy và sắp xếp địa chỉ thành công.");
                    onComplete.accept(true, locationList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi lấy danh sách địa chỉ: " + e.getMessage());
                    onComplete.accept(false, Collections.emptyList());
                });
    }

    /**
     * Hàm hỗ trợ ánh xạ thủ công từ Map sang UserLocationEntity
     */
    private UserLocationEntity mapToUserLocationEntity(Map<String, Object> map) {
        UserLocationEntity location = new UserLocationEntity();

        // Đảm bảo kiểu dữ liệu: Double từ Firestore có thể là Long/Double.
        // Sử dụng ((Number) map.get(key)).doubleValue() là cách an toàn.

        location.setId((String) map.get("id"));
        location.setLocationType((String) map.get("locationType"));
        location.setAddress((String) map.get("address"));
//        location.setRecipientName((String) map.get("recipientName"));
        location.setPhoneNumber((String) map.get("phoneNumber"));

        // Xử lý các trường số (double/boolean)
        Object latValue = map.get("latitude");
        if (latValue instanceof Number) {
            location.setLatitude(((Number) latValue).doubleValue());
        }

        Object lngValue = map.get("longitude");
        if (lngValue instanceof Number) {
            location.setLongitude(((Number) lngValue).doubleValue());
        }

        // Trường Boolean
        Object isDefaultValue = map.get("defaultLocation");
        if (isDefaultValue instanceof Boolean) {
            location.setDefaultLocation((Boolean) isDefaultValue);
        }

        // TODO: Thêm các trường khác nếu có

        return location;
    }


    public void checkUserHasLocations(String uid, BiConsumer<Boolean, String> onComplete) {
        if (uid == null || uid.isEmpty()) {
            onComplete.accept(false, "UID rỗng");
            return;
        }

        DocumentReference userDocRef = db.collection(DatabaseTable.USER_LOCATIONS.getValue()).document(uid);

        userDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Kiểm tra xem có field "locations" không và có ít nhất 1 phần tử
                        List<?> locations = (List<?>) documentSnapshot.get("locations");
                        if (locations != null && !locations.isEmpty()) {
                            Log.d(TAG, "✅ UID: " + uid + " đã có " + locations.size() + " bản ghi location.");
                            onComplete.accept(true, "Đã có bản ghi location");
                        } else {
                            Log.d(TAG, "⚠️ UID: " + uid + " chưa có bản ghi location nào.");
                            onComplete.accept(false, "Chưa có bản ghi location");
                        }
                    } else {
                        Log.d(TAG, "⚠️ Document UID: " + uid + " chưa tồn tại trong collection.");
                        onComplete.accept(false, "Document chưa tồn tại");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi khi kiểm tra location: " + e.getMessage());
                    onComplete.accept(false, e.getMessage());
                });
    }

}
