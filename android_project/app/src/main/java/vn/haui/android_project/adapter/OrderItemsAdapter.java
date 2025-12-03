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
import java.util.Locale;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.ItemOrderProduct;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.ViewHolder> {

    private final List<ItemOrderProduct> items;

    public OrderItemsAdapter(List<ItemOrderProduct> items) {
        this.items = items;
    }

    // Lớp ViewHolder giữ các view trong layout item_order_detail_rich.xml
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Cần thêm TextView để hiển thị số lượng (ví dụ: tv_item_quantity)
        public final TextView itemName;
        public final TextView itemDetails;
        public final TextView itemPrice;
        public final TextView itemQuantity; // Đã thêm: TextView cho số lượng
        public final ImageView itemPhoto;

        // Đã thêm: Biến Context để sử dụng cho Glide và getIdentifier
        private final Context context;

        public ViewHolder(View itemView) {
            super(itemView);
            this.context = itemView.getContext();
            itemName = itemView.findViewById(R.id.tv_item_name);
            itemDetails = itemView.findViewById(R.id.tv_item_details);
            itemPrice = itemView.findViewById(R.id.tv_item_price);
            itemQuantity = itemView.findViewById(R.id.tv_item_quantity); // Giả định ID của TextView số lượng
            itemPhoto = itemView.findViewById(R.id.img_item_photo);
        }

        public void bind(ItemOrderProduct item) {
            DecimalFormat formatter = new DecimalFormat("#,###");
            if (item.getName() != null) {
                itemName.setText(item.getName());
            }
            if (item.getDetails() != null) {
                itemDetails.setText(item.getDetails());
            }
            itemPrice.setText(formatter.format(item.getTotalPrice()) + "đ");
            itemQuantity.setText("x " + item.getQuantity());
            String imageName = item.getImage();
            if (imageName != null && !imageName.isEmpty()) {
//                String resourceName = imageName.replace(".png", "")
//                        .replace(".jpg", "")
//                        .trim()
//                        .toLowerCase(Locale.getDefault());
                loadPreviewImage(imageName, itemPhoto);

//                int imageResourceId = context.getResources().getIdentifier(
//                        resourceName,
//                        "drawable",
//                        context.getPackageName()
//                );
//                if (imageResourceId > 0) {
//                    Glide.with(context)
//                            .load(imageResourceId)
//                            .placeholder(R.drawable.image_breakfast)
//                            .error(R.drawable.image_breakfast)
//                            .into(itemPhoto);
//                } else {
//                    Glide.with(context).load(R.drawable.image_breakfast).into(itemPhoto);
//                }
            } else {
                // Nếu tên ảnh bị rỗng/null, tải ảnh fallback
                Glide.with(context).load(R.drawable.image_breakfast).into(itemPhoto);
            }
        }
    }

    private static void loadPreviewImage(String url, ImageView imageView) {
        if (url == null || url.isEmpty()) return;

        GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                .build());

        Glide.with(imageView.getContext())
                .load(glideUrl)
                .override(600, 600)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(imageView);
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemOrderProduct item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}