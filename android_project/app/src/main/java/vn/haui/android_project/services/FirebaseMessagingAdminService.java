package vn.haui.android_project.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color; // Thêm import này
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map; // Thêm import này

import vn.haui.android_project.R;
import vn.haui.android_project.view.AdminScreenActivity;
import vn.haui.android_project.view.EmployeeScreenActivity;

public class FirebaseMessagingAdminService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.d("FCM_Admin", "Nhận thông báo: " + message.getFrom());

        // Lấy dữ liệu data trước (quan trọng để chuyển màn hình)
        Map<String, String> data = message.getData();

        // 1. Trường hợp thông báo có cả Notification Payload và Data
        if (message.getNotification() != null) {
            String title = message.getNotification().getTitle();
            String body = message.getNotification().getBody();
            sendNotification(title, body, data);
        }
        // 2. Trường hợp chỉ có Data Payload (thường dùng khi server tự custom hoàn toàn)
        else if (data.size() > 0) {
            String title = data.get("title");
            String body = data.get("body");
            // Nếu không có title trong data thì lấy mặc định
            if (title == null) title = "Thông báo mới";
            if (body == null) body = "Bạn có đơn hàng mới cần xử lý";

            sendNotification(title, body, data);
        }
    }

    // Đã thêm tham số Map<String, String> data để truyền dữ liệu chuyển tab
    private void sendNotification(String title, String body, Map<String, String> data) {
        String channelId = "admin_order_channel_v2";        // --- SỬA ĐOẠN NÀY: KIỂM TRA QUYỀN ĐỂ CHỌN MÀN HÌNH ĐÍCH ---
        Class<?> targetActivity;

        // Giả sử bạn lưu role trong SharedPreferences tên là "AppPrefs"
        // Bạn hãy thay "AppPrefs" và logic lấy role theo đúng code Login của bạn
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String role = prefs.getString("role", "admin"); // Mặc định là admin hoặc user tùy bạn

        if ("employee".equals(role)) {
            // Nếu là nhân viên -> Mở màn hình nhân viên
            targetActivity = EmployeeScreenActivity.class; // <-- Thay bằng tên Activity của nhân viên
        } else {
            // Nếu là admin -> Mở màn hình Admin
            targetActivity = AdminScreenActivity.class;
        }

        Intent intent = new Intent(this, targetActivity);
        // -----------------------------------------------------------

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Giữ nguyên phần putExtra
        if (data != null && data.containsKey("open_fragment")) {
            intent.putExtra("open_fragment", data.get("open_fragment"));
        }
        if (data != null && data.containsKey("order_id")) {
            intent.putExtra("order_id", data.get("order_id"));
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Ưu tiên cao cho Android cũ
                .setDefaults(NotificationCompat.DEFAULT_ALL)   // Rung, chuông mặc định
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // Hiện nội dung trên màn hình khóa

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Cấu hình Channel cho Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Thông báo đơn hàng (Admin)", // Tên hiển thị trong cài đặt
                    NotificationManager.IMPORTANCE_HIGH // --- QUAN TRỌNG ĐỂ HIỆN POPUP ---
            );
            channel.setDescription("Nhận thông báo khi có đơn hàng mới");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);

            manager.createNotificationChannel(channel);
        }

        // Dùng ID ngẫu nhiên để không bị ghi đè thông báo cũ
        int notificationId = (int) System.currentTimeMillis();
        manager.notify(notificationId, builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM_Admin", "Token mới: " + token);
    }
}
