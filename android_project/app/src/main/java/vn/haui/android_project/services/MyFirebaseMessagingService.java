package vn.haui.android_project.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;
import vn.haui.android_project.enums.DatabaseTable;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_SERVICE";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() != null) {
            showNotification(remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody());
        }
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            Log.d(TAG, "Message data payload: " + data);

            // Lấy thông tin đơn hàng từ data payload
            String orderId = data.get("orderId");
            String pickupAddress = data.get("pickupAddress");
            String deliveryAddress = data.get("deliveryAddress");

            // Kiểm tra xem có phải là thông báo "đơn hàng mới" không
            // (Bạn có thể tự quy định 'type' khi gửi từ Admin)
            if (data.get("type") != null && data.get("type").equals("NEW_ORDER")) {
                // Gửi một broadcast cục bộ
                // Chỉ những Activity đang chạy và đăng ký lắng nghe mới nhận được
                sendNewOrderBroadcast(orderId, pickupAddress, deliveryAddress);
            }

            // Nếu bạn muốn hiện thông báo trên thanh status (khi app ở background)
            // thì thêm code tạo Notification tại đây.
        }
    }
    private void sendNewOrderBroadcast(String orderId, String pickup, String delivery) {
        Intent intent = new Intent("com.yourpackage.NEW_ORDER"); // Đặt tên action của bạn
        intent.putExtra("orderId", orderId);
        intent.putExtra("pickupAddress", pickup);
        intent.putExtra("deliveryAddress", delivery);

        // Dùng LocalBroadcastManager để chỉ gửi trong nội bộ app
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Token mới: " + token);
        saveTokenToFirestore(token);
    }

    private void saveTokenToFirestore(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("token", token);
        deviceData.put("timestamp", System.currentTimeMillis());

        // Lưu danh sách token theo user
        db.collection(DatabaseTable.USERS.getValue())
                .document(uid)
                .collection("devices")
                .document(token)
                .set(deviceData)
                .addOnSuccessListener(a -> Log.d(TAG, "Đã lưu token thiết bị"))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi lưu token", e));
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, "default_channel")
                        .setSmallIcon(R.drawable.ic_abount_yumyard)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        manager.notify(0, builder.build());
    }
}
