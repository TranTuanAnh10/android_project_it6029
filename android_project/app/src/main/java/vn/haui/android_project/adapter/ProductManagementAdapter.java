package vn.haui.android_project.adapter;import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
// Bỏ import Glide, chúng ta không dùng nữa
// import com.bumptech.glide.Glide;
import java.text.DecimalFormat;
import java.util.List;
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

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_management, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductItem product = productList.get(position);
        holder.bind(product, listener);
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public void updateData(List<ProductItem> newProducts) {
        if (newProducts != null) {
            this.productList.clear();
            this.productList.addAll(newProducts);
            notifyDataSetChanged();
        }
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName, tvProductPrice, tvProductStatus;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.iv_product_image_manage);
            tvProductName = itemView.findViewById(R.id.tv_product_name_manage);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price_manage);
            tvProductStatus = itemView.findViewById(R.id.tv_product_status_manage);
        }

        void bind(final ProductItem product, final OnProductClickListener listener) {
            tvProductName.setText(product.getName());

            DecimalFormat formatter = new DecimalFormat("#,###");
            String formattedPrice = formatter.format(product.getPrice()) + "đ";
            tvProductPrice.setText(formattedPrice);

            String status = product.getStatus() != null ? product.getStatus() : "Còn hàng";
            tvProductStatus.setText(status);
            if ("Còn hàng".equalsIgnoreCase(status)) {
                tvProductStatus.setBackgroundColor(Color.parseColor("#4CAF50")); // Màu xanh
            } else {
                tvProductStatus.setBackgroundColor(Color.parseColor("#F44336")); // Màu đỏ
            }

            // **[SỬA LỖI TẠI ĐÂY]** Load ảnh từ drawable bằng tên file
            String imageName = product.getImage();
            int index = imageName.lastIndexOf('.');
            if (index != -1) {
                imageName = imageName.substring(0, index);
            }
            if (imageName != null && !imageName.isEmpty()) {
                // Dùng getIdentifier để chuyển String tên ảnh thành ID tài nguyên
                int imageResourceId = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());

                if (imageResourceId != 0) {
                    // Nếu tìm thấy ảnh, gán nó cho ImageView
                    ivProductImage.setImageResource(imageResourceId);
                } else {
                    // Nếu không tìm thấy, hiển thị ảnh placeholder
                    Log.w("Adapter", "Không tìm thấy ảnh: " + imageName + " trong drawable.");
                    ivProductImage.setImageResource(R.drawable.img_placeholder);
                }
            } else {
                // Nếu trường image bị rỗng, cũng hiển thị ảnh placeholder
                ivProductImage.setImageResource(R.drawable.img_placeholder);
            }

            itemView.setOnClickListener(v -> listener.onProductClick(product));
        }
    }
}
