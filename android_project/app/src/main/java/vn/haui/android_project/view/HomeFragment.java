package vn.haui.android_project.view;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.textfield.TextInputEditText;

import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.Cart;
import vn.haui.android_project.entity.CartItem;
import vn.haui.android_project.entity.CategoryItem;
import vn.haui.android_project.entity.ProductItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {
    // Khai báo biến ở đây để có thể truy cập trong onResume
    private TextInputEditText edtSearch;
    LinearLayout topPickLayout, bestSellerLayout;

    public HomeFragment() {
    }

    private static final String COLLECTION_CATEGORYS = "categorys";
    private static final String COLLECTION_PRODUCTS = "products";
    View view;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        this.view = view;
        mapping();
        db = FirebaseFirestore.getInstance();
        loadAllHomeData();
        mAuth = FirebaseAuth.getInstance();
        return view;
    }
    private void mapping(){
        topPickLayout = view.findViewById(R.id.layoutTopPicks);
        bestSellerLayout = view.findViewById(R.id.layoutBestSelling);
    }

    private void loadAllHomeData() {

        db.collection(COLLECTION_CATEGORYS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<CategoryItem> topPicksList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            CategoryItem item = document.toObject(CategoryItem.class);
                            item.setId(document.getId());
                            topPicksList.add(item);
                        }
                        populateCuisineLayout(topPicksList);
                    } else {
                        Log.w(TAG, "Lỗi khi tải Top Picks", task.getException());
                    }
                });
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
                        List<ProductItem> topPickLists = productItems.stream().filter(item -> item.getRate() > 4.5).collect(Collectors.toList());
                        populateProductLayout(topPickLists, topPickLayout, 0);
                        List<ProductItem> bestSellingList = new ArrayList<>();
                        List<ProductItem> listCopy = new ArrayList<>(productItems);
                        Collections.shuffle(listCopy);
                        int countItemUsed = Math.min(5, listCopy.size());
                        List<ProductItem> randomUniqueItems = listCopy.subList(0, countItemUsed);

                        populateProductLayout(randomUniqueItems, bestSellerLayout, 1);
                        populateProductReadyToLunch(productItems);
                        Log.d(TAG, "Tải Top Picks thành công: " + productItems.size() + " mục");

                    } else {
                        Log.w(TAG, "Lỗi khi tải Top Picks", task.getException());
                    }
                });
    }

    public void populateCuisineLayout(List<CategoryItem> items) {
        LinearLayout parentLayout = view.findViewById(R.id.layoutCuisines);
        parentLayout.removeAllViews();

        Context context = view.getContext();
        parentLayout.setWeightSum(items.size());

        int imageSizePx = dpToPx(context, 48);
        int textMarginTopPx = dpToPx(context, 8);

        for (CategoryItem item : items) {
            LinearLayout itemLayout = new LinearLayout(context);

            LinearLayout.LayoutParams itemLayoutParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            itemLayout.setLayoutParams(itemLayoutParams);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setGravity(Gravity.CENTER);

            ShapeableImageView imageView = new ShapeableImageView(context);
            LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(
                    imageSizePx,
                    imageSizePx
            );
            imageView.setLayoutParams(imageLayoutParams);
            String drawableName = item.getImage();
            int drawableId = context.getResources().getIdentifier(
                    drawableName,
                    "drawable",
                    context.getPackageName()
            );
            imageView.setImageResource(drawableId);

            MaterialTextView textView = new MaterialTextView(context);
            LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            textLayoutParams.topMargin = textMarginTopPx;
            textView.setLayoutParams(textLayoutParams);
            textView.setText(item.getName());


            itemLayout.addView(imageView);
            itemLayout.addView(textView);
            final CategoryItem currentItem = item;
            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onCuisineItemClicked(currentItem);
                }
            });
            parentLayout.addView(itemLayout);
        }
    }
    private void populateProductLayout(List<ProductItem> items, LinearLayout parent, int type) {
        if (parent == null || getContext() == null) {
            Log.e(TAG, "Parent LinearLayout cho Product bị null");
            return;
        }


        parent.removeAllViews();
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        for (ProductItem item : items) {
            View itemView = inflater.inflate(R.layout.item_product, parent, false);

            ImageView ivProductImage = itemView.findViewById(R.id.ivProductImage);
            ImageView toprate = itemView.findViewById(R.id.iv_icon_type);
            TextView bestSelling = itemView.findViewById(R.id.iv_text_type);
//            TextView tvProductCategory = itemView.findViewById(R.id.tvProductCategory);
            TextView tvProductName = itemView.findViewById(R.id.tvProductName);
//            TextView tvProductRating = itemView.findViewById(R.id.tvProductRating);
            TextView tvProductPrice = itemView.findViewById(R.id.tvProductPrice);

            switch (type){
                case 0:
                    toprate.setImageResource(R.drawable.ic_tag);
                    bestSelling.setText("Top Rate");
                    bestSelling.setTextColor(ContextCompat.getColor(view.getContext(), R.color.color_text_home_page_red));
                    break;
                case 1:
                    toprate.setImageResource(R.drawable.ic_trending_up);
                    bestSelling.setText("Best Selling");
                    bestSelling.setTextColor(ContextCompat.getColor(view.getContext(), R.color.color_text_home_page_green));
                    break;
            }
            tvProductName.setText(item.getName());
//            tvProductCategory.setText(item.getCategory());
//            tvProductRating.setText(String.valueOf(item.getRate()));

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
    private void populateProductReadyToLunch(List<ProductItem> items) {
        LinearLayout parent = view.findViewById(R.id.layoutReadyForLunch);
        if (parent == null || getContext() == null) {
            Log.e(TAG, "Parent LinearLayout cho Product bị null");
            return;
        }

        parent.removeAllViews();
        Context context = getContext();
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

    private void onProductItemClick(ProductItem item){
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

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
            Toast.makeText(getContext(), "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(getContext(), "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Lỗi ghi RTDB", e));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Lỗi đọc RTDB", error.toException());
            }
        });
    }
    private void onCuisineItemClicked(CategoryItem item) {
        Toast.makeText(requireContext(), "Bạn đã chọn: " + item.getName(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), SearchResultActivity.class);
        intent.putExtra("category", item.getName());
        startActivity(intent);
    }
    private static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtSearch = view.findViewById(R.id.edtSearch);

        edtSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SearchResultActivity.class);

                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (edtSearch != null) {
            edtSearch.clearFocus();
        }
    }
}