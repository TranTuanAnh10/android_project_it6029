package vn.haui.android_project.view;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

// Import các lớp cần thiết từ dự án của bạn
import vn.haui.android_project.R;
import vn.haui.android_project.adapter.NotificationAdapter;
import vn.haui.android_project.entity.Notification;


import java.util.ArrayList;
import java.util.List;

/**
 * Fragment hiển thị màn hình Danh sách Thông báo (Notifications)
 */
public class NotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;

    public NotificationsFragment() {
        // Required empty public constructor
    }

    // ... (Phần newInstance và onCreate giữ nguyên) ...

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 1. Inflate Layout và giữ lại View gốc của Fragment
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 2. Khởi tạo và thiết lập RecyclerView
        recyclerView = view.findViewById(R.id.notifications_recycler_view);

        // Thiết lập Layout Manager (cần thiết cho RecyclerView)
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 3. Chuẩn bị dữ liệu và thiết lập Adapter
        notificationList = prepareNotificationData();
        adapter = new NotificationAdapter(getContext(), notificationList);
        recyclerView.setAdapter(adapter);

        // 4. Thiết lập sự kiện cho các View khác trong Fragment
        setupEventListeners(view);
    }

    private void setupEventListeners(View view) {

    }

    // Hàm tạo dữ liệu mẫu (Mock Data)
    private List<Notification> prepareNotificationData() {
        // Trong dự án thực tế, bạn sẽ lấy dữ liệu từ ViewModel/Repository/API
        List<Notification> list = new ArrayList<>();

        // Cần thay R.drawable.xxx và R.color.xxx bằng các tài nguyên thực tế của bạn

        // --- Nhóm 1: New updates
        list.add(new Notification("New updates"));

        list.add(new Notification(
                "Adam West",
                "Hi, I'm on my way",
                "Just now",
                true,
                R.drawable.ic_abount_yumyard,
                R.color.red_primary
        ));

        list.add(new Notification(
                "Your Order is on its Way!",
                "Your order is now out for delivery. Our driver is en route to bring you your fresh...",
                "Just now",
                true,
                R.drawable.ic_abount_yumyard,
                R.color.red_primary
        ));

        // --- Nhóm 2: Older
        list.add(new Notification("Older"));

        list.add(new Notification(
                "Limited Time Offer Inside!",
                "Hungry for savings? We've got you covered! Check out our latest offer: Buy...",
                "Fri",
                false,
                R.drawable.ic_tag,
                R.color.red_primary
        ));

        list.add(new Notification(
                "App Maintenance Update",
                "Attention, foodies! We’re conducting routine maintenance to improve your ap...",
                "a week ago",
                false,
                R.drawable.ic_abount_yumyard,
                R.color.red_primary
        ));

        return list;
    }
}