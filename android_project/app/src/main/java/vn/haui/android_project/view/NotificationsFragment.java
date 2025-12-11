package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.NotificationAdapter;
import vn.haui.android_project.entity.NotificationEntity;
import vn.haui.android_project.model.NotificationItem;
import vn.haui.android_project.services.FirebaseNotificationService;

/**
 * Fragment hiển thị màn hình Danh sách Thông báo (Notifications)
 * Đã được sửa lỗi và bỏ phần loading.
 */
public class NotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private static final String TAG = "NotificationsFragment";

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private FirebaseNotificationService notificationService;

    // Danh sách để hiển thị, chứa cả header và item. Luôn được khởi tạo.
    private List<NotificationItem> displayList = new ArrayList<>();

    // View để hiển thị trạng thái rỗng hoặc lỗi

    public NotificationsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ Views
        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        // 2. Khởi tạo các đối tượng cần thiết
        notificationService = new FirebaseNotificationService(); // <<--- SỬA LỖI 1: KHỞI TẠO SERVICE
        // 3. Thiết lập RecyclerView với danh sách chính xác
        setupRecyclerView();

        // 4. Tải dữ liệu từ Firestore
        loadNotificationsFromFirebase();
    }
    @Override
    public void onNotificationItemClick(NotificationEntity entity) {
        // Kiểm tra để đảm bảo entity và getContext() không null
        if (entity == null || getContext() == null) {
            return;
        }
        // Logic điều hướng dựa trên type của thông báo
        if ("ORDER_STATUS".equals(entity.getType())) {
            Intent intent = new Intent(getContext(), OrderDetailsActivity.class);
            intent.putExtra("ORDER_ID", entity.getTargetId());
            startActivity(intent);
        } else if ("VOUCHER".equals(entity.getType())) {
            Toast.makeText(getContext(), "Mở màn hình khuyến mãi...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Đã nhấn vào: " + entity.getTitle(), Toast.LENGTH_SHORT).show();
        }
        notificationService.markAsRead(entity.getId());
    }
    private void setupRecyclerView() {
        // Truyền 'this' vì Fragment này đã implement OnNotificationClickListener
        adapter = new NotificationAdapter(getContext(), displayList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadNotificationsFromFirebase() {
        // Ban đầu, ẩn RecyclerView và hiển thị thông báo "Đang tải..."
        recyclerView.setVisibility(View.GONE);

        notificationService.getCurrentUserNotifications((notifications, exception) -> {
            if (getContext() == null || !isAdded()) {
                return; // Fragment đã bị hủy, không làm gì cả
            }

            if (exception != null) {
                Log.e(TAG, "Lỗi khi tải thông báo: ", exception);
                showStateMessage("Lỗi tải dữ liệu. Vui lòng thử lại.");
                return;
            }

            if (notifications == null || notifications.isEmpty()) {
                Log.d(TAG, "Không có thông báo nào.");
                showStateMessage("Bạn chưa có thông báo nào.");
            } else {
                Log.d(TAG, "Tải thành công " + notifications.size() + " thông báo.");
                recyclerView.setVisibility(View.VISIBLE);
                processAndGroupNotifications(notifications);
            }
        });
    }

    private void processAndGroupNotifications(List<NotificationEntity> notifications) {
        displayList.clear(); // Xóa dữ liệu cũ trước khi thêm mới

        List<NotificationEntity> unreadList = new ArrayList<>();
        List<NotificationEntity> readList = new ArrayList<>();

        for (NotificationEntity entity : notifications) {
            if (entity.isRead()) {
                readList.add(entity);
            } else {
                unreadList.add(entity);
            }
        }

        if (!unreadList.isEmpty()) {
            displayList.add(NotificationItem.asHeader("Mới"));
            for (NotificationEntity entity : unreadList) {
                displayList.add(NotificationItem.asItem(entity));
            }
        }

        if (!readList.isEmpty()) {
            displayList.add(NotificationItem.asHeader("Cũ hơn"));
            for (NotificationEntity entity : readList) {
                displayList.add(NotificationItem.asItem(entity));
            }
        }

        // <<--- SỬA LỖI 3: THÔNG BÁO CHO ADAPTER DỮ LIỆU ĐÃ THAY ĐỔI
        adapter.notifyDataSetChanged();
    }

    // Hàm helper để quản lý trạng thái rỗng/lỗi
    private void showStateMessage(String message) {
        recyclerView.setVisibility(View.GONE);
    }
}
