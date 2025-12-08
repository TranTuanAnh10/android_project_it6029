package vn.haui.android_project.adapter;


import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.Notification;

// Hằng số để phân biệt View Type (Quan trọng cho nhóm header)
public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final List<Notification> notifications;
    private final Context context;

    public NotificationAdapter(Context context, List<Notification> notifications) {
        this.context = context;
        this.notifications = notifications;
    }

    // ----------------------------------------------------
    // PHÂN BIỆT VIEW TYPE
    // ----------------------------------------------------
    @Override
    public int getItemViewType(int position) {
        if (notifications.get(position).isGroupHeader()) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    // ----------------------------------------------------
    // KHỞI TẠO VIEWHOLDER (Inflate Layout)
    // ----------------------------------------------------
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            // Layout cho Tiêu đề nhóm
            View view = LayoutInflater.from(context).inflate(R.layout.header_notification, parent, false);
            return new HeaderViewHolder(view);
        } else {
            // Layout cho Mục thông báo
            View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
            return new ItemViewHolder(view);
        }
    }

    // ----------------------------------------------------
    // BIND DỮ LIỆU
    // ----------------------------------------------------
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.tvHeaderTitle.setText(notification.getHeaderTitle());
        } else {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            // Đặt dữ liệu
            itemHolder.tvTitle.setText(notification.getTitle());
            itemHolder.tvSubtitle.setText(notification.getSubtitle());
            itemHolder.tvTime.setText(notification.getTime());

            // Xử lý Icon và Màu sắc
            itemHolder.ivIcon.setImageResource(notification.getIconResId());

            // Đặt màu cho icon (dùng PorterDuff.Mode.SRC_IN)
            int iconColor = ContextCompat.getColor(context, notification.getIconColorResId());
            itemHolder.ivIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);

            // Xử lý trạng thái Chưa đọc
            if (notification.isUnread()) {
                itemHolder.vUnreadDot.setVisibility(View.VISIBLE);
                // Đặt màu nền cho mục (tùy chọn theo thiết kế)
                // itemHolder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.unread_background));
                itemHolder.tvTitle.setTypeface(null, android.graphics.Typeface.BOLD); // In đậm tiêu đề
            } else {
                itemHolder.vUnreadDot.setVisibility(View.GONE);
                // itemHolder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                itemHolder.tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            // Xử lý sự kiện click
            itemHolder.itemView.setOnClickListener(v -> {
                // Thêm logic xử lý khi người dùng nhấn vào thông báo
                // Ví dụ: Mở chi tiết thông báo, đánh dấu đã đọc
            });
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    // ----------------------------------------------------
    // VIEWHOLDER CHO HEADER (Tiêu đề nhóm)
    // ----------------------------------------------------
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderTitle;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            // Cần có layout notification_header.xml (xem phía dưới)
            tvHeaderTitle = itemView.findViewById(R.id.tv_header_title);
        }
    }

    // ----------------------------------------------------
    // VIEWHOLDER CHO ITEM (Mục thông báo)
    // ----------------------------------------------------
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvTime;
        ImageView ivIcon;
        View vUnreadDot;
        FrameLayout iconContainer;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            vUnreadDot = itemView.findViewById(R.id.v_unread_dot);
            iconContainer = itemView.findViewById(R.id.icon_container);
        }
    }
}
