package vn.haui.android_project.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.text.DecimalFormat;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.ItemOrderProduct;

// Adapter riêng dành cho phần Quản lý đơn hàng (Load ảnh bằng URL)
public class OrderManagementItemsAdapter extends RecyclerView.Adapter<OrderManagementItemsAdapter.ViewHolder> {

    private final List<ItemOrderProduct> items;
    private Context context;

    public OrderManagementItemsAdapter(List<ItemOrderProduct> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        // Dùng lại layout item_order_detail (hoặc item_order_product tùy project bạn)
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemOrderProduct item = items.get(position);
        holder.bind(item, context);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView itemName;
        public final TextView itemDetails;
        public final TextView itemPrice;
        public final TextView itemQuantity;
        public final ImageView itemPhoto;

        public ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.tv_item_name);
            itemDetails = itemView.findViewById(R.id.tv_item_details);
            itemPrice = itemView.findViewById(R.id.tv_item_price);
            itemQuantity = itemView.findViewById(R.id.tv_item_quantity);
            itemPhoto = itemView.findViewById(R.id.img_item_photo);
        }

        public void bind(ItemOrderProduct item, Context context) {
            // 1. Tên
            if (item.getName() != null) {
                itemName.setText(item.getName());
            }

            // 2. Chi tiết
            if (item.getDetails() != null && !item.getDetails().isEmpty()) {
                itemDetails.setText(item.getDetails());
                itemDetails.setVisibility(View.VISIBLE);
            } else {
                itemDetails.setVisibility(View.GONE);
            }

            // 3. Giá tiền
            DecimalFormat formatter = new DecimalFormat("#,###");
            itemPrice.setText(formatter.format(item.getTotalPrice()) + "đ");

            // 4. Số lượng
            itemQuantity.setText("x " + item.getQuantity());

            // 5. XỬ LÝ LOAD ẢNH TỪ URL (Khác biệt chính so với Adapter cũ)
            String imgUrl = item.getImage();

            if (imgUrl != null && !imgUrl.isEmpty()) {
                // Thêm User-Agent giả lập trình duyệt để tránh lỗi 403
                GlideUrl glideUrl = new GlideUrl(imgUrl, new LazyHeaders.Builder()
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                        .build());

                Glide.with(context)
                        .load(glideUrl)
                        .placeholder(R.drawable.image_breakfast) // Ảnh chờ khi đang tải
                        .error(R.drawable.image_breakfast)       // Ảnh hiển thị khi lỗi
                        .into(itemPhoto);
            } else {
                // Nếu không có link ảnh thì hiện ảnh mặc định
                Glide.with(context)
                        .load(R.drawable.image_breakfast)
                        .into(itemPhoto);
            }
        }
    }
}
