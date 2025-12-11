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
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.haui.android_project.callbacks.NotificationListCallback;
import vn.haui.android_project.callbacks.VoucherUserListCallback;
import vn.haui.android_project.entity.NotificationEntity;
import vn.haui.android_project.entity.VoucherEntity;
import vn.haui.android_project.enums.DatabaseTable;

public class FirebaseVoucherUserService {
    private static final String TAG = "FirebaseVoucherUserService";
    private final FirebaseFirestore db;
    private final CollectionReference voucherUserRef;
    private final FirebaseAuth mAuth;

    public FirebaseVoucherUserService() {
        db = FirebaseFirestore.getInstance();
        voucherUserRef = db.collection(DatabaseTable.VOUCHER_USER.getValue());
        mAuth = FirebaseAuth.getInstance();
    }


    public void addNotification(String uid, @NonNull VoucherEntity voucherEntity) {
        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "UID rỗng, không thể thêm thông báo.");
            return;
        }
        DocumentReference userDocRef = voucherUserRef.document(uid);
        voucherEntity.setId(voucherEntity.getId());
        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snapshot = transaction.get(userDocRef);
                    Map<String, Object> notificationMap;
                    try {
                        // Giả sử bạn đã có phương thức fromMap, chúng ta có thể dùng Gson để chuyển đổi ngược lại
                        // Đây là một cách an toàn để đảm bảo tất cả các trường đều đúng định dạng
                        Gson gson = new Gson();
                        String json = gson.toJson(voucherEntity);
                        notificationMap = gson.fromJson(json, Map.class);
                    } catch (Exception e) {
                        Log.e(TAG, "Không thể chuyển đổi VoucherEntity sang Map", e);
                        throw new FirebaseFirestoreException("Lỗi chuyển đổi object", FirebaseFirestoreException.Code.INTERNAL);
                    }
                    if (snapshot.exists()) {
                        transaction.update(userDocRef, DatabaseTable.VOUCHER_USER.getValue(), FieldValue.arrayUnion(notificationMap));
                    } else {
                        Map<String, Object> data = new HashMap<>();
                        data.put(DatabaseTable.VOUCHER_USER.getValue(), Collections.singletonList(notificationMap));
                        transaction.set(userDocRef, data);
                    }
                    return null;
                })
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ (Transaction) Xử lý thông báo thành công cho UID: " + uid))
                .addOnFailureListener(e -> Log.e(TAG, "❌ (Transaction) Lỗi khi xử lý thông báo cho UID " + uid + ": " + e.getMessage()));
    }


    public void getCurrentVoucherUser(@NonNull VoucherUserListCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Không có người dùng nào đang đăng nhập.");
            callback.onComplete(Collections.emptyList(), new Exception("User not logged in."));
            return;
        }
        String uid = currentUser.getUid();
        voucherUserRef.document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onComplete(Collections.emptyList(), null);
                        return;
                    }
                    Object voucherUserObject = documentSnapshot.get(DatabaseTable.VOUCHER_USER.getValue());
                    if (!(voucherUserObject instanceof List)) {
                        callback.onComplete(Collections.emptyList(), null);
                        return;
                    }
                    List<Map<String, Object>> rawVoucherUser = (List<Map<String, Object>>) voucherUserObject;
                    List<String> unreadList = new ArrayList<>();
                    for (Map<String, Object> notificationMap : rawVoucherUser) {
                        VoucherEntity entity = VoucherEntity.fromMap(notificationMap);
                        unreadList.add(entity.getId());
                    }
                    Log.d(TAG, "✅ Lấy và sắp xếp " + unreadList.size() + " thông báo (tối đa 5 chưa đọc, 5 đã đọc).");
                    callback.onComplete(unreadList, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Lỗi khi lấy danh sách thông báo: " + e.getMessage());
                    callback.onComplete(Collections.emptyList(), e);
                });
    }
}
