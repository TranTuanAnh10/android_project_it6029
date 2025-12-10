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
                        transaction.update(userDocRef, "notifications", FieldValue.arrayUnion(notificationMap));
                    } else {
                        // Nếu document chưa tồn tại, tạo mới document với một mảng chứa thông báo đầu tiên
                        Map<String, Object> data = new HashMap<>();
                        data.put("notifications", Collections.singletonList(notificationMap));
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
                batch.update(userDocRef, "notifications", FieldValue.arrayUnion(newNotification));
            }
        }
        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ (Batch) Đã gửi thành công thông báo đến " + uids.size() + " người dùng."))
                .addOnFailureListener(e -> Log.e(TAG, "❌ (Batch) Lỗi khi gửi thông báo hàng loạt: " + e.getMessage() + ". Có thể một số user chưa có document."));
    }

    /**
     * Lấy danh sách thông báo cho người dùng đang đăng nhập.
     * Trả về kết quả qua một callback đơn giản.
     */
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
                    Object notificationsObject = documentSnapshot.get("notifications");
                    if (!(notificationsObject instanceof List)) {
                        callback.onComplete(Collections.emptyList(), null);
                        return;
                    }

                    List<NotificationEntity> notificationList = new ArrayList<>();
                    List<Map<String, Object>> rawNotifications = (List<Map<String, Object>>) notificationsObject;

                    for (Map<String, Object> notificationMap : rawNotifications) {
                        NotificationEntity entity = NotificationEntity.fromMap(notificationMap);
                        if (entity != null) {
                            notificationList.add(entity);
                        }
                    }

                    notificationList.sort(Comparator.comparing(NotificationEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
                    Log.d(TAG, "✅ Lấy và sắp xếp " + notificationList.size() + " thông báo cho người dùng hiện tại.");
                    callback.onComplete(notificationList, null);
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
        final Gson gson = new Gson();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snapshot = transaction.get(userDocRef);
                    if (!snapshot.exists()) {
                        throw new FirebaseFirestoreException("Document người dùng không tồn tại.", FirebaseFirestoreException.Code.NOT_FOUND);
                    }
                    List<Map<String, Object>> rawNotifications = (List<Map<String, Object>>) snapshot.get("notifications");
                    if (rawNotifications == null) {
                        throw new FirebaseFirestoreException("Danh sách notifications rỗng.", FirebaseFirestoreException.Code.NOT_FOUND);
                    }
                    List<NotificationEntity> newNotifications = new ArrayList<>();
                    boolean found = false;
                    for (Map<String, Object> map : rawNotifications) {
                        NotificationEntity currentNotif = gson.fromJson(gson.toJson(map), NotificationEntity.class);
                        if (notificationId.equals(currentNotif.getId())) {
                            currentNotif.setRead(true);
                            found = true;
                        }
                        newNotifications.add(currentNotif);
                    }
                    if (!found) {
                        throw new FirebaseFirestoreException("Không tìm thấy thông báo với ID: " + notificationId, FirebaseFirestoreException.Code.NOT_FOUND);
                    }
                    transaction.update(userDocRef, "notifications", newNotifications);
                    return null;
                })
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đánh dấu đã đọc thành công cho thông báo: " + notificationId))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi khi đánh dấu đã đọc: " + e.getMessage()));
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
