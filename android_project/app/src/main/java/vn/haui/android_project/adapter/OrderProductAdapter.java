package vn.haui.android_project.adapter;
import android.content.Context; // Import Context
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

public class OrderProductAdapter extends RecyclerView.Adapter<OrderProductAdapter.ProductViewHolder> {

    private final List<ItemOrderProduct> productList;

    public OrderProductAdapter(List<ItemOrderProduct> productList) {
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
        ItemOrderProduct product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        public ImageView imgProductThumb;
        public TextView tvProductNameQty,tvProductName;
        public TextView tvProductDetails;
        public TextView tvProductPrice;

        private final Context context;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            // Lấy Context từ itemView
            this.context = itemView.getContext();
            tvProductName=itemView.findViewById(R.id.tv_product_name);
            imgProductThumb = itemView.findViewById(R.id.img_product_thumb);
            tvProductNameQty = itemView.findViewById(R.id.tv_product_name_qty);
            tvProductDetails = itemView.findViewById(R.id.tv_product_details);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price);
        }

        public void bind(ItemOrderProduct product) {
            tvProductName.setText(product.getName());
            tvProductNameQty.setText("Số lượng: " + product.getQuantity());
            tvProductDetails.setText(product.getDetails());
            DecimalFormat formatter = new DecimalFormat("#,###");
            String priceText = formatter.format(product.getTotalPrice()) + "đ";
            tvProductPrice.setText(priceText);
            loadPreviewImage(product.getImage(), imgProductThumb);
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
    }
}