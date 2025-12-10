package vn.haui.android_project.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.NotificationEntity;
import vn.haui.android_project.model.NotificationItem;
import vn.haui.android_project.view.OrderDetailsActivity;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final List<NotificationItem> itemList;
    private final Context context;
    public interface OnNotificationClickListener {
        void onNotificationItemClick(NotificationEntity entity);
    }

    private final OnNotificationClickListener clickListener;
    public NotificationAdapter(Context context, List<NotificationItem> itemList, OnNotificationClickListener listener) {
        this.context = context;
        this.itemList = itemList;
        this.clickListener = listener; // Gán listener
    }

    @Override
    public int getItemViewType(int position) {
        if (itemList.get(position).isGroupHeader()) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.header_notification, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_notification, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // <<--- SỬA LỖI 1: LẤY ĐÚNG ĐỐI TƯỢNG NotificationItem
        NotificationItem currentItem = itemList.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            // Lấy tiêu đề header từ NotificationItem
            headerHolder.tvHeaderTitle.setText(currentItem.getHeaderTitle());
        } else {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            // <<--- SỬA LỖI 2: LẤY ĐỐI TƯỢNG NotificationEntity TỪ BÊN TRONG NotificationItem
            NotificationEntity entity = currentItem.getEntity();

            // Kiểm tra null để tránh crash (dù không nên xảy ra)
            if (entity == null) {
                Log.e("NotificationAdapter", "NotificationEntity is null for an item view at position: " + position);
                return;
            }

            // --- Bắt đầu bind dữ liệu từ NotificationEntity ---

            // 1. Đặt dữ liệu văn bản
            itemHolder.tvTitle.setText(entity.getTitle());
            itemHolder.tvSubtitle.setText(entity.getBody());

            // Format và hiển thị thời gian
            if (entity.getCreatedAt() != null) {
                // Ví dụ: hiển thị "dd/MM/yyyy HH:mm"
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault());
                itemHolder.tvTime.setText(sdf.format(entity.getCreatedAt()));
            } else {
                itemHolder.tvTime.setText(""); // Hoặc "vừa xong"
            }

            // 2. Xử lý Icon và Màu sắc dựa trên type
            int iconResId;
            switch (entity.getType() != null ? entity.getType() : "") {
                case "ORDER_STATUS":
                    iconResId = R.drawable.ic_box; // Thay bằng icon của bạn
                    break;
                case "PROMOTION":
                    iconResId = R.drawable.ic_tag;
                    break;
                case "SYSTEM_UPDATE":
                    iconResId = R.drawable.ic_abount_yumyard;
                    break;
                default:
                    iconResId = R.drawable.ic_feedback;
                    break;
            }
            itemHolder.ivIcon.setImageResource(iconResId);


            // 3. <<--- SỬA LỖI 3: XỬ LÝ ĐÚNG LOGIC TRẠNG THÁI "ĐÃ ĐỌC"
            if (entity.isRead()) {
                // ĐÃ ĐỌC: Ẩn chấm tròn, chữ bình thường
                itemHolder.vUnreadDot.setVisibility(View.GONE);
                itemHolder.tvTitle.setTypeface(null, Typeface.NORMAL);
            } else {
                // CHƯA ĐỌC: Hiển thị chấm tròn, chữ in đậm
                itemHolder.vUnreadDot.setVisibility(View.VISIBLE);
                itemHolder.tvTitle.setTypeface(null, Typeface.BOLD);
            }

            // 4. Xử lý sự kiện click
            itemHolder.itemView.setOnClickListener(v -> {
                // Kiểm tra listener có tồn tại không để tránh NullPointerException
                if (clickListener != null) {
                    // Gọi callback về cho Fragment, gửi kèm đối tượng entity đã được click
                    clickListener.onNotificationItemClick(entity);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        // Phải kiểm tra null để phòng trường hợp Fragment truyền vào danh sách null
        return itemList != null ? itemList.size() : 0;
    }

    // ----- Các ViewHolder không thay đổi -----
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderTitle;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeaderTitle = itemView.findViewById(R.id.tv_header_title);
        }
    }

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
            ivIcon = itemView.findViewById(R.id.iv_icon_noti);
            vUnreadDot = itemView.findViewById(R.id.v_unread_dot);
            iconContainer = itemView.findViewById(R.id.icon_container);
        }
    }
}
