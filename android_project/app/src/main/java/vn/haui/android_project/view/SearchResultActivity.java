package vn.haui.android_project.view;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;
import vn.haui.android_project.entity.Cart;
import vn.haui.android_project.entity.CartItem;
import vn.haui.android_project.entity.CategoryItem;
import vn.haui.android_project.entity.ProductItem;

public class SearchResultActivity extends AppCompatActivity {
    private TextInputEditText etSearchQuery;
    private LinearLayout mainLayout;
    private ImageView btnBack;
    private static final String COLLECTION_PRODUCTS = "products";
    private FirebaseFirestore db;

    private FirebaseAuth mAuth;
    private String category = "";
    List<ProductItem> productItems;

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
        Intent intent = getIntent();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadAllHomeData();

        // Ánh xạ các view
        etSearchQuery = findViewById(R.id.edtSearch);
        mainLayout = findViewById(R.id.main);
        btnBack = findViewById(R.id.btnBack);
        category = intent.getStringExtra("category");

        mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearchQuery.clearFocus();
                searchItem("");
            }
        });

        etSearchQuery.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                    String searchText = etSearchQuery.getText().toString().trim();
                    category = "";
                    searchItem(searchText);
                    hideKeyboard(v);
                    return true;
                }
                return false;
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
    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void loadPreviewImage(String url, ImageView imageView) {
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
                        this.productItems = productItems;
                        searchItem("");
                    } else {
                        Log.w(TAG, "Lỗi khi tải Top Picks", task.getException());
                    }
                });
    }
    private void onProductItemClick(ProductItem item){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Xác nhận");
        builder.setMessage("Bạn có chắc chắn muốn thêm "+ item.getName() + " vào giỏ hàng không?");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                addToCartRealtimeDB(item);
            }
        });

        builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void addToCartRealtimeDB(ProductItem clickedProduct) {
        if (mAuth == null || mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference cartRef = FirebaseDatabase.getInstance()
                .getReference("carts")
                .child(userId);

        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Cart cart = snapshot.getValue(Cart.class);

                if (cart == null) {
                    cart = new Cart(new ArrayList<>());
                }

                boolean itemFound = false;
                if (cart.items == null) cart.items = new ArrayList<>();

                for (CartItem item : cart.items) {
                    if (item.item_details.getId().equals(clickedProduct.getId())) {
                        item.quantity = item.getQuantity() + 1;
                        itemFound = true;
                        break;
                    }
                }

                if (!itemFound) {
                    cart.items.add(new CartItem(clickedProduct, 1));
                }

                cartRef.setValue(cart)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(SearchResultActivity.this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Lỗi ghi RTDB", e));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Lỗi đọc RTDB", error.toException());
            }
        });
    }

    private void searchItem(String searchText){
        List<ProductItem> newList = getItemByCategory();
        String cleanSearchText = searchText.toLowerCase().trim();
        if(!searchText.isBlank() || !searchText.isEmpty()){
            newList = newList.stream()
                    .filter(x -> x.getName().toLowerCase().contains(cleanSearchText)) // Dùng contains()
                    .collect(Collectors.toList()); }
        populateProductReadyToLunch(newList);
    }

    public List<ProductItem> getItemByCategory(){
        if(category == null || category.isEmpty() || category.isBlank()){
            return productItems;
        }
        else
            return productItems.stream().filter(x -> x.getCategory().equals(category)).collect(Collectors.toList());
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


            ivType.setText(R.string.top_rate);

            tvProductName.setText(item.getName());
            tvProductDesc.setText(item.getDescription());
            tvProductCategory.setText(item.getCategory());
            tvProductRating.setText(String.valueOf(item.getRate()));

            DecimalFormat formatter = new DecimalFormat("#,###");
            String formattedPrice = formatter.format(item.getPrice()) + "đ";
            tvProductPrice.setText(formattedPrice);
//
//            String itemName = item.getImage();
//            int index = itemName.lastIndexOf('.');
//            if (index != -1) {
//                itemName = itemName.substring(0, index);
//            }
//            int drawableId = context.getResources().getIdentifier(
//                    itemName,
//                    "drawable",
//                    context.getPackageName()
//            );
//            ivProductImage.setImageResource(drawableId);
//            Glide.with(context)
//                    .load(drawableId)
//                    .placeholder(R.drawable.img_cake)
//                    .error(R.drawable.ic_burger)
//                    .into(ivProductImage);
                loadPreviewImage(item.getImage(), ivProductImage);
            itemView.setOnClickListener(v -> {
                onProductItemClick(item);
            });

            parent.addView(itemView);
        }
    }
}
