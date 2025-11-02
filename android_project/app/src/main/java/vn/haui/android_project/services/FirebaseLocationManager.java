package vn.haui.android_project.services;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.google.gson.Gson;

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

    public void updateLocation(String uid, UserLocationEntity updatedLocation, BiConsumer<Boolean, String> onComplete) {
        DocumentReference userDocRef = db.collection(DatabaseTable.USER_LOCATIONS.getValue()).document(uid);
        String targetId = updatedLocation.getId();
        final Gson gson = new Gson(); // Khởi tạo Gson
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(userDocRef);
            if (!snapshot.exists()) {
                try {
                    throw new Exception("Document người dùng không tồn tại.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            List<Map<String, Object>> rawLocations = (List<Map<String, Object>>) snapshot.get("locations");
            if (rawLocations == null) {
                try {
                    throw new Exception("Danh sách locations rỗng. Không tìm thấy ID để cập nhật.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            List<UserLocationEntity> currentLocations = new ArrayList<>();
            for (Map<String, Object> map : rawLocations) {
                String jsonString = gson.toJson(map);
                UserLocationEntity location = gson.fromJson(jsonString, UserLocationEntity.class);
                currentLocations.add(location);
            }
            List<UserLocationEntity> newLocations = new ArrayList<>(currentLocations);
            boolean found = false;
            for (int i = 0; i < newLocations.size(); i++) {
                UserLocationEntity existingLocation = newLocations.get(i);
                if (targetId.equals(existingLocation.getId())) {
                    newLocations.set(i, updatedLocation); // Thay thế đối tượng cũ
                    found = true;
                    break;
                }
            }
            if (!found) {
                try {
                    throw new Exception("Không tìm thấy location với ID: " + targetId + " để cập nhật.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            transaction.update(userDocRef, "locations", newLocations);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Cập nhật location thành công cho UID: " + uid + ", ID: " + targetId);
            onComplete.accept(true, uid);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi cập nhật location: " + e.getMessage());
            onComplete.accept(false, "Cập nhật thất bại: " + e.getMessage());
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
        location.setId((String) map.get("id"));
        location.setLocationType((String) map.get("locationType"));
        location.setAddress((String) map.get("address"));
        location.setPhoneNumber((String) map.get("phoneNumber"));
        Object latValue = map.get("latitude");
        if (latValue instanceof Number) {
            location.setLatitude(((Number) latValue).doubleValue());
        }
        Object lngValue = map.get("longitude");
        if (lngValue instanceof Number) {
            location.setLongitude(((Number) lngValue).doubleValue());
        }
        Object isDefaultValue = map.get("defaultLocation");
        if (isDefaultValue instanceof Boolean) {
            location.setDefaultLocation((Boolean) isDefaultValue);
        }
        location.setRecipientName((String) map.get("recipientName"));
        location.setCountry((String) map.get("country"));
        location.setZipCode((String) map.get("zipCode"));
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


    public void hasDefaultLocation(String uid, String excludeLocationId, BiConsumer<Boolean, Boolean> onComplete) {
        if (uid == null || uid.isEmpty()) {
            onComplete.accept(false, false); // success=false, hasDefault=false
            return;
        }

        DocumentReference userDocRef = db.collection(DatabaseTable.USER_LOCATIONS.getValue()).document(uid);

        userDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        onComplete.accept(true, false);
                        return;
                    }

                    List<Map<String, Object>> rawLocations = (List<Map<String, Object>>) documentSnapshot.get("locations");

                    if (rawLocations == null || rawLocations.isEmpty()) {
                        onComplete.accept(true, false);
                        return;
                    }

                    // Duyệt qua danh sách để kiểm tra trường isDefaultLocation
                    for (Map<String, Object> map : rawLocations) {
                        Boolean isDefault = (Boolean) map.get("defaultLocation");
                        String currentId = (String) map.get("id"); // Cần lấy ID của bản ghi hiện tại

                        boolean isExcluded = excludeLocationId != null &&
                                currentId != null &&
                                currentId.equals(excludeLocationId);

                        if (!isExcluded && isDefault != null && isDefault) {
                            onComplete.accept(true, true); // Thành công, và CÓ địa chỉ mặc định khác
                            return;
                        }
                    }

                    onComplete.accept(true, false); // Thành công, nhưng KHÔNG có địa chỉ mặc định khác
                })
                .addOnFailureListener(e -> {
                    onComplete.accept(false, false); // Thất bại, không có địa chỉ mặc định
                });
    }
    public void deleteLocationById(String uid, String locationId, BiConsumer<Boolean, String> onComplete) {
        if (uid == null || uid.isEmpty() || locationId == null || locationId.isEmpty()) {
            onComplete.accept(false, "UID hoặc Location ID rỗng");
            return;
        }
        Gson gson = new Gson();
        DocumentReference userDocRef = db.collection(DatabaseTable.USER_LOCATIONS.getValue()).document(uid);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(userDocRef);
            List<UserLocationEntity> currentLocations = new ArrayList<>();

            // 1. Đọc dữ liệu hiện tại
            if (snapshot.exists()) {
                List<Map<String, Object>> rawLocations = (List<Map<String, Object>>) snapshot.get("locations");
                if (rawLocations != null) {
                    for (Map<String, Object> map : rawLocations) {
                        currentLocations.add(gson.fromJson(gson.toJson(map), UserLocationEntity.class));
                    }
                }
            }

            // 2. Tìm và xóa địa điểm theo ID
            // Sử dụng removeIf để xóa đối tượng thỏa mãn điều kiện
            boolean wasRemoved = currentLocations.removeIf(loc -> loc.getId().equals(locationId));

            if (!wasRemoved) {
                // Đây là lỗi nếu bạn chắc chắn ID tồn tại, nhưng không ảnh hưởng đến Transaction
                try {
                    throw new Exception("Location ID không được tìm thấy trong danh sách.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // 3. Ghi đè toàn bộ danh sách đã cập nhật (đã xóa)
            transaction.set(userDocRef, Map.of("locations", currentLocations));

            return null; // Trả về null khi Transaction thành công
        }).addOnSuccessListener(aVoid -> {
            // Log.d(TAG, "✅ Xóa location thành công: " + locationId);
            onComplete.accept(true, "Xóa thành công.");
        }).addOnFailureListener(e -> {
            // Log.e(TAG, "❌ Lỗi transaction khi xóa location: " + e.getMessage());
            onComplete.accept(false, "Lỗi xóa địa điểm: " + e.getMessage());
        });
    }
}
