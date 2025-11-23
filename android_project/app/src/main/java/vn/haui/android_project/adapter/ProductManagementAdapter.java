package vn.haui.android_project.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.ProductItem;

public class ProductManagementAdapter extends RecyclerView.Adapter<ProductManagementAdapter.ProductViewHolder> {

    private Context context;
    private List<ProductItem> productList;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(ProductItem product);
    }

    public ProductManagementAdapter(Context context, List<ProductItem> productList, OnProductClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    // Hàm để Fragment gọi khi cần refresh dữ liệu
    public void updateData(List<ProductItem> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // SỬA: Gọi đúng tên file layout của bạn là item_product_management
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_management, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductItem product = productList.get(position);

        // 1. Hiển thị Tên
        holder.tvName.setText(product.getName());

        // 2. Hiển thị Giá (Format: 50,000 đ)
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedPrice = formatter.format(product.getPrice()) + "đ";
        holder.tvPrice.setText(formattedPrice);

        // 3. XỬ LÝ TRẠNG THÁI (STATUS) - Nền xanh/đỏ, chữ trắng
        String status = product.getStatus();

        // Tạo khung bo tròn cho trạng thái
        GradientDrawable backgroundShape = new GradientDrawable();
        backgroundShape.setShape(GradientDrawable.RECTANGLE);
        backgroundShape.setCornerRadius(12f); // Bo góc

        // Map trạng thái: "available" hoặc "Còn hàng" -> Xanh, còn lại -> Đỏ
        if ("available".equalsIgnoreCase(status) || "Còn hàng".equalsIgnoreCase(status)) {
            holder.tvStatus.setText("Còn hàng");
            backgroundShape.setColor(Color.parseColor("#4CAF50")); // Xanh lá
        } else {
            holder.tvStatus.setText("Hết hàng");
            backgroundShape.setColor(Color.parseColor("#F44336")); // Đỏ
        }

        // Set màu chữ trắng và background
        holder.tvStatus.setTextColor(Color.WHITE);
        holder.tvStatus.setBackground(backgroundShape);


        // 4. LOAD ẢNH TỪ LINK (Sử dụng Glide) vào iv_product_image_manage
        String imageUrl = product.getImage();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .override(300, 300) // Resize cho nhẹ list
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Sự kiện click
        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    // --- ViewHolder: Ánh xạ đúng các ID trong file item_product_management.xml ---
    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvStatus;
        ImageView imgProduct;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            // SỬA: Các ID này lấy từ file XML bạn vừa gửi
            imgProduct = itemView.findViewById(R.id.iv_product_image_manage);
            tvName = itemView.findViewById(R.id.tv_product_name_manage);
            tvPrice = itemView.findViewById(R.id.tv_product_price_manage);
            tvStatus = itemView.findViewById(R.id.tv_product_status_manage);
        }
    }
}
