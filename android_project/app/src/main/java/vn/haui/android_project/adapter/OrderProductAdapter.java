package vn.haui.android_project.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import vn.haui.android_project.R;
import vn.haui.android_project.entity.OrderProduct;

public class OrderProductAdapter extends RecyclerView.Adapter<OrderProductAdapter.ProductViewHolder> {

    private final List<OrderProduct> productList;

    public OrderProductAdapter(List<OrderProduct> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        OrderProduct product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        public ImageView imgProductThumb;
        public TextView tvProductNameQty;
        public TextView tvProductDetails;
        public TextView tvProductPrice;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProductThumb = itemView.findViewById(R.id.img_product_thumb);
            tvProductNameQty = itemView.findViewById(R.id.tv_product_name_qty);
            tvProductDetails = itemView.findViewById(R.id.tv_product_details);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price);
        }

        public void bind(OrderProduct product) {
            // Cập nhật tên và số lượng
            String nameWithQty = product.getName() + " x" + product.getQuantity();
            tvProductNameQty.setText(nameWithQty);

            // Cập nhật chi tiết
            tvProductDetails.setText(product.getDetails());

            // Cập nhật tổng giá
            String priceText = "$" + String.format("%.2f", product.getTotalPrice());
            tvProductPrice.setText(priceText);

            // Tùy chọn: Đặt ảnh placeholder (Bạn cần tạo drawable tên là placeholder_thumb)
            // imgProductThumb.setImageResource(R.drawable.placeholder_thumb);
        }
    }
}