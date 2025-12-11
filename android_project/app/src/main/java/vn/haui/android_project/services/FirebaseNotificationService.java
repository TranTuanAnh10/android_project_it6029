package vn.haui.android_project.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.haui.android_project.callbacks.NotificationListCallback;
import vn.haui.android_project.entity.NotificationEntity;
import vn.haui.android_project.enums.DatabaseTable;

public class FirebaseNotificationService {
    private static final String TAG = "FirebaseNotificationMgr";
    private final FirebaseFirestore db;
    private final CollectionReference notificationsRef;
    private final FirebaseAuth mAuth;

    public FirebaseNotificationService() {
        db = FirebaseFirestore.getInstance();
        notificationsRef = db.collection(DatabaseTable.NOTIFICATIONS.getValue());
        mAuth = FirebaseAuth.getInstance();
    }


    /**
     * Thêm một thông báo mới cho MỘT UID cụ thể.
     * Sử dụng Transaction để đảm bảo tính toàn vẹn dữ liệu, tránh bị ghi đè.
     * (Không cần callback, kiểu "bắn và quên").
     */
    public void addNotification(String uid, @NonNull NotificationEntity newNotification) {
        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "UID rỗng, không thể thêm thông báo.");
            return;
        }

        DocumentReference userDocRef = notificationsRef.document(uid);
        newNotification.setId(String.valueOf(System.currentTimeMillis()));
        newNotification.setCreatedAt(new Date());
        // Chạy một transaction để đảm bảo thao tác đọc-sửa-ghi được an toàn
        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snapshot = transaction.get(userDocRef);

                    // Chuyển đổi đối tượng NotificationEntity thành một Map mà Firestore có thể hiểu được.
                    // Điều này rất quan trọng để FieldValue.arrayUnion hoạt động ổn định.
                    // Bạn cần đảm bảo class NotificationEntity có phương thức toMap().
                    Map<String, Object> notificationMap;
                    try {
                        // Giả sử bạn đã có phương thức fromMap, chúng ta có thể dùng Gson để chuyển đổi ngược lại
                        // Đây là một cách an toàn để đảm bảo tất cả các trường đều đúng định dạng
                        Gson gson = new Gson();
                        String json = gson.toJson(newNotification);
                        notificationMap = gson.fromJson(json, Map.class);
                    } catch (Exception e) {
                        Log.e(TAG, "Không thể chuyển đổi NotificationEntity sang Map", e);
                        // Nếu không chuyển đổi được, không làm gì cả để tránh lỗi
                        throw new FirebaseFirestoreException("Lỗi chuyển đổi object", FirebaseFirestoreException.Code.INTERNAL);
                    }

                    if (snapshot.exists()) {
                        // Nếu document của người dùng đã tồn tại, thêm thông báo mới vào mảng "notifications"
                        transaction.update(userDocRef, DatabaseTable.NOTIFICATIONS.getValue(), FieldValue.arrayUnion(notificationMap));
                    } else {
                        // Nếu document chưa tồn tại, tạo mới document với một mảng chứa thông báo đầu tiên
                        Map<String, Object> data = new HashMap<>();
                        data.put(DatabaseTable.NOTIFICATIONS.getValue(), Collections.singletonList(notificationMap));
                        transaction.set(userDocRef, data);
                    }
                    // Transaction yêu cầu phải trả về một giá trị, trả về null khi thành công
                    return null;
                })
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ (Transaction) Xử lý thông báo thành công cho UID: " + uid))
                .addOnFailureListener(e -> Log.e(TAG, "❌ (Transaction) Lỗi khi xử lý thông báo cho UID " + uid + ": " + e.getMessage()));
    }


    /**
     * Thêm một thông báo cho NHIỀU người dùng cùng lúc.
     * @param uids Danh sách các UID của người nhận.
     * @param newNotification Đối tượng thông báo cần gửi.
     */
    public void addNotification(@NonNull List<String> uids, @NonNull NotificationEntity newNotification) {
        if (uids.isEmpty()) {
            Log.w(TAG, "Danh sách UID rỗng, không có hành động nào được thực hiện.");
            return;
        }

        WriteBatch batch = db.batch();
        newNotification.setId(String.valueOf(System.currentTimeMillis()));

        for (String uid : uids) {
            if (uid != null && !uid.isEmpty()) {
                DocumentReference userDocRef = notificationsRef.document(uid);
                // Thao tác này sẽ thất bại nếu document chưa tồn tại.
                // Để đảm bảo, một giải pháp nâng cao hơn là dùng Cloud Function để đọc-rồi-ghi.
                // Tuy nhiên, với logic hiện tại, chúng ta giả định document đã có hoặc sẽ được tạo khi user dùng app.
                batch.update(userDocRef, DatabaseTable.NOTIFICATIONS.getValue(), FieldValue.arrayUnion(newNotification));
            }
        }
        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ (Batch) Đã gửi thành công thông báo đến " + uids.size() + " người dùng."))
                .addOnFailureListener(e -> Log.e(TAG, "❌ (Batch) Lỗi khi gửi thông báo hàng loạt: " + e.getMessage() + ". Có thể một số user chưa có document."));
    }

    public void getCurrentUserNotifications(@NonNull NotificationListCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Không có người dùng nào đang đăng nhập.");
            callback.onComplete(Collections.emptyList(), new Exception("User not logged in."));
            return;
        }

        String uid = currentUser.getUid();

        notificationsRef.document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onComplete(Collections.emptyList(), null);
                        return;
                    }
                    Object notificationsObject = documentSnapshot.get(DatabaseTable.NOTIFICATIONS.getValue());
                    if (!(notificationsObject instanceof List)) {
                        callback.onComplete(Collections.emptyList(), null);
                        return;
                    }

                    // --- LOGIC MỚI BẮT ĐẦU TỪ ĐÂY ---

                    List<Map<String, Object>> rawNotifications = (List<Map<String, Object>>) notificationsObject;
                    List<NotificationEntity> unreadList = new ArrayList<>();
                    List<NotificationEntity> readList = new ArrayList<>();

                    // 1. Phân loại tất cả thông báo vào 2 danh sách: unread và read
                    for (Map<String, Object> notificationMap : rawNotifications) {
                        NotificationEntity entity = NotificationEntity.fromMap(notificationMap);
                        if (entity != null) {
                            if (entity.isRead()) {
                                readList.add(entity);
                            } else {
                                unreadList.add(entity);
                            }
                        }
                    }

                    // 2. Sắp xếp mỗi danh sách theo thời gian mới nhất
                    // Dùng Comparator.nullsLast để tránh crash nếu có ngày null
                    Comparator<NotificationEntity> dateComparator = Comparator.comparing(
                            NotificationEntity::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    );
                    unreadList.sort(dateComparator);
                    readList.sort(dateComparator);

                    // 3. Lấy 5 item hàng đầu từ mỗi danh sách (hoặc ít hơn nếu danh sách không đủ 5)
                    List<NotificationEntity> top5Unread = unreadList.subList(0, Math.min(5, unreadList.size()));
                    List<NotificationEntity> top5Read = readList.subList(0, Math.min(5, readList.size()));

                    // 4. Gộp hai danh sách đã được giới hạn lại với nhau
                    List<NotificationEntity> finalList = new ArrayList<>();
                    finalList.addAll(top5Unread);
                    finalList.addAll(top5Read);

                    Log.d(TAG, "✅ Lấy và sắp xếp " + finalList.size() + " thông báo (tối đa 5 chưa đọc, 5 đã đọc).");
                    callback.onComplete(finalList, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi khi lấy danh sách thông báo: " + e.getMessage());
                    callback.onComplete(Collections.emptyList(), e);
                });
    }


    /**
     * Đánh dấu một thông báo là đã đọc cho người dùng ĐANG ĐĂNG NHẬP.
     */
    public void markAsRead(@NonNull String notificationId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Không có người dùng nào đang đăng nhập để đánh dấu đã đọc.");
            return;
        }
        String uid = currentUser.getUid();
        DocumentReference userDocRef = notificationsRef.document(uid);
        // Không cần Gson ở đây nữa, chúng ta sẽ dùng hàm toMap()

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snapshot = transaction.get(userDocRef);
                    if (!snapshot.exists()) {
                        throw new FirebaseFirestoreException("Document người dùng không tồn tại.", FirebaseFirestoreException.Code.NOT_FOUND);
                    }

                    // Lấy danh sách thô từ Firestore
                    Object rawObject = snapshot.get(DatabaseTable.NOTIFICATIONS.getValue());
                    if (!(rawObject instanceof List)) {
                        throw new FirebaseFirestoreException("Trường 'notifications' không phải là một danh sách hoặc không tồn tại.", FirebaseFirestoreException.Code.INVALID_ARGUMENT);
                    }
                    List<Map<String, Object>> originalNotifications = (List<Map<String, Object>>) rawObject;

                    // Danh sách mới để chứa các Map đã được cập nhật
                    List<Map<String, Object>> updatedNotificationsAsMap = new ArrayList<>();
                    boolean foundAndUpdated = false;

                    // Lặp qua danh sách gốc
                    for (Map<String, Object> notificationMap : originalNotifications) {
                        // Lấy ID từ map
                        Object idObj = notificationMap.get("id");
                        if (idObj != null && notificationId.equals(idObj.toString())) {
                            // TÌM THẤY: Cập nhật trường 'read' thành true
                            notificationMap.put("read", true);
                            foundAndUpdated = true;
                        }
                        // Thêm map (dù đã sửa hay chưa) vào danh sách mới
                        updatedNotificationsAsMap.add(notificationMap);
                    }

                    if (!foundAndUpdated) {
                        // Nếu không tìm thấy thông báo, không làm gì cả để tránh lỗi
                        Log.w(TAG, "Không tìm thấy thông báo với ID: " + notificationId + " để đánh dấu đã đọc.");
                        // Không ném lỗi để tránh làm crash các flow khác, chỉ ghi log
                        return null;
                    }

                    // <<--- SỬA LỖI TẠI ĐÂY ---
                    // Cập nhật lại toàn bộ mảng với danh sách các Map đã được sửa
                    transaction.update(userDocRef, "notifications", updatedNotificationsAsMap);
                    return null;
                })
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ (Transaction) Đánh dấu đã đọc thành công cho thông báo: " + notificationId))
                .addOnFailureListener(e -> Log.e(TAG, "❌ (Transaction) Lỗi khi đánh dấu đã đọc: " + e.getMessage()));
    }


    /**
     * Xóa một thông báo theo ID cho người dùng ĐANG ĐĂNG NHẬP.
     */
    public void deleteNotificationById(@NonNull String notificationId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Không có người dùng nào đang đăng nhập để xóa thông báo.");
            return;
        }

        String uid = currentUser.getUid();
        DocumentReference userDocRef = notificationsRef.document(uid);
        Gson gson = new Gson();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snapshot = transaction.get(userDocRef);
                    if (!snapshot.exists()) {
                        throw new FirebaseFirestoreException("Document người dùng không tồn tại.", FirebaseFirestoreException.Code.NOT_FOUND);
                    }

                    List<Map<String, Object>> rawList = (List<Map<String, Object>>) snapshot.get("notifications");
                    if (rawList == null) {
                        throw new FirebaseFirestoreException("Danh sách notifications rỗng.", FirebaseFirestoreException.Code.NOT_FOUND);
                    }

                    List<NotificationEntity> currentNotifications = new ArrayList<>();
                    for (Map<String, Object> map : rawList) {
                        currentNotifications.add(gson.fromJson(gson.toJson(map), NotificationEntity.class));
                    }

                    boolean wasRemoved = currentNotifications.removeIf(notif -> notif.getId().equals(notificationId));
                    if (!wasRemoved) {
                        throw new FirebaseFirestoreException("Notification ID không được tìm thấy để xóa.", FirebaseFirestoreException.Code.NOT_FOUND);
                    }
                    transaction.update(userDocRef, "notifications", currentNotifications);
                    return null;
                })
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Xóa thông báo thành công: " + notificationId))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi transaction khi xóa thông báo: " + e.getMessage()));
    }
}
