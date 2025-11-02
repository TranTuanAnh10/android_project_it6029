package vn.haui.android_project.view;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.CategoryItem;
import vn.haui.android_project.entity.ProductItem;

public class SearchResultActivity extends AppCompatActivity {
    private EditText etSearchQuery;
    private LinearLayout mainLayout;
    private ImageView btnBack;
    private static final String COLLECTION_PRODUCTS = "products";
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_result);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        db = FirebaseFirestore.getInstance();
        loadAllHomeData();

        // Ánh xạ các view
        etSearchQuery = findViewById(R.id.edtSearch);
        mainLayout = findViewById(R.id.main);
        btnBack = findViewById(R.id.btnBack); // <====== THÊM DÒNG NÀY ĐỂ SỬA LỖI

        // Xóa focus khỏi EditText khi người dùng chạm vào vùng khác (ĐÃ ĐÚNG)
        mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearchQuery.clearFocus();
            }
        });

        // Xử lý sự kiện click cho nút back
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadAllHomeData() {
        db.collection(COLLECTION_PRODUCTS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<ProductItem> productItems = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ProductItem item = document.toObject(ProductItem.class);
                            item.setId(document.getId());
                            productItems.add(item);
                        }
                        populateProductReadyToLunch(productItems);
                    } else {
                        Log.w(TAG, "Lỗi khi tải Top Picks", task.getException());
                    }
                });
    }

    private void onProductItemClick(ProductItem item){
        Toast.makeText(this, "Bạn đã chọn: " + item.getName(), Toast.LENGTH_SHORT).show();
    }

    private void populateProductReadyToLunch(List<ProductItem> items) {
        LinearLayout parent = findViewById(R.id.layoutReadyForLunch);
        if (parent == null || this == null) {
            Log.e(TAG, "Parent LinearLayout cho Product bị null");
            return;
        }

        parent.removeAllViews();
        Context context = this;
        LayoutInflater inflater = LayoutInflater.from(context);

        for (ProductItem item : items) {
            View itemView = inflater.inflate(R.layout.item_product_full, parent, false);

            ImageView ivProductImage = itemView.findViewById(R.id.rfl_image);
            TextView ivType = itemView.findViewById(R.id.rfl_tag);
            TextView tvProductCategory = itemView.findViewById(R.id.rfl_category);
            TextView tvProductDesc = itemView.findViewById(R.id.rfl_desc);
            TextView tvProductName = itemView.findViewById(R.id.rfl_name);
            TextView tvProductRating = itemView.findViewById(R.id.rfl_rate);
            TextView tvProductPrice = itemView.findViewById(R.id.rfl_price);


            ivType.setText("Top Rate");

            tvProductName.setText(item.getName());
            tvProductDesc.setText(item.getDescription());
            tvProductCategory.setText(item.getCategory());
            tvProductRating.setText(String.valueOf(item.getRate()));

            DecimalFormat formatter = new DecimalFormat("#,###");
            String formattedPrice = formatter.format(item.getPrice()) + "đ";
            tvProductPrice.setText(formattedPrice);

            String itemName = item.getImage();
            int index = itemName.lastIndexOf('.');
            if (index != -1) {
                itemName = itemName.substring(0, index);
            }
            int drawableId = context.getResources().getIdentifier(
                    itemName,
                    "drawable",
                    context.getPackageName()
            );
            ivProductImage.setImageResource(drawableId);
//            Glide.with(context)
//                    .load(drawableId)
//                    .placeholder(R.drawable.img_cake)
//                    .error(R.drawable.ic_burger)
//                    .into(ivProductImage);

            itemView.setOnClickListener(v -> {
                onProductItemClick(item);
            });

            parent.addView(itemView);
        }
    }
}
