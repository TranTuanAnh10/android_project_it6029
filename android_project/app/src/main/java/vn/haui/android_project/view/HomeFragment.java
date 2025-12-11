package vn.haui.android_project.view;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

import vn.haui.android_project.R;
import vn.haui.android_project.entity.Cart;
import vn.haui.android_project.entity.CartItem;
import vn.haui.android_project.entity.CategoryItem;
import vn.haui.android_project.entity.ItemOrderProduct;
import vn.haui.android_project.entity.ProductItem;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.services.FirebaseUserManager;

public class HomeFragment extends Fragment {
    // Khai báo biến ở đây để có thể truy cập trong onResume
    private TextInputEditText edtSearch;
    TextView tv_hello;
    LinearLayout topPickLayout, bestSellerLayout;
    FrameLayout loadingPanel;
    private FrameLayout fabCart;
    private DatabaseReference cartRef;
    private Animation shakeAnimation;

    public HomeFragment() {
    }

    private static final String COLLECTION_CATEGORYS = "categorys";
    private static final String COLLECTION_PRODUCTS = "products";
    View view;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    FirebaseUser authUser;
    private ValueEventListener cartValueEventListener;
    MaterialTextView tv_countCard;
    FloatingActionButton actionButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        this.view = view;
        mapping();
        authUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        loadAllHomeData();
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            cartRef = FirebaseDatabase.getInstance()
                    .getReference(DatabaseTable.CART.getValue())
                    .child(mAuth.getCurrentUser().getUid());
        }
        checkCartData();
        if (fabCart != null) {
            // Ban đầu ẩn FAB, sẽ được checkCartData() hiện lên nếu có sản phẩm
            fabCart.setVisibility(View.GONE);
            actionButton.setOnClickListener(v -> {
                Intent intent = new Intent(this.getContext(), ConfirmPaymentActivity.class); // Dùng this.getContext()
                startActivity(intent);

            });
        }
        return view;
    }

    private void mapping() {
        topPickLayout = view.findViewById(R.id.layoutTopPicks);
        bestSellerLayout = view.findViewById(R.id.layoutBestSelling);
        loadingPanel = view.findViewById(R.id.loadingOverlay);
        tv_hello = view.findViewById(R.id.greeting_message);
        fabCart = view.findViewById(R.id.count_card);
        tv_countCard = view.findViewById(R.id.tv_cart_count_badge);
        actionButton = view.findViewById(R.id.fab_cart);
        // Cập nhật để tải animation mới (tên file giả định là jump_animation)
        if (getContext() != null) {
            shakeAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.shake_animation);
        }
    }

    private void loadAllHomeData() {
        if (authUser == null) return;
        FirebaseUserManager userManager = new FirebaseUserManager();
        userManager.getUserByUid(authUser.getUid(), userData -> {
            String name = (String) userData.getOrDefault("name", authUser.getDisplayName());
            tv_hello.setText("Xin chào " + (name != null ? name : "bạn") + " \uD83D\uDC4B");
        }, error -> {
            tv_hello.setText("Xin chào bạn \uD83D\uDC4B");
        });
        showLoading(true);
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

                        showLoading(false);
                    } else {
                        Log.w(TAG, "Lỗi khi tải Top Picks", task.getException());

                        showLoading(false);
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

            loadPreviewImage(item.getImage(),imageView);

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

            switch (type) {
                case 0:
                    toprate.setImageResource(R.drawable.ic_tag);
                    bestSelling.setText(R.string.top_rate);
                    bestSelling.setTextColor(ContextCompat.getColor(view.getContext(), R.color.color_text_home_page_red));
                    break;
                case 1:
                    toprate.setImageResource(R.drawable.ic_trending_up);
                    bestSelling.setText(R.string.best_selling);
                    bestSelling.setTextColor(ContextCompat.getColor(view.getContext(), R.color.color_text_home_page_green));
                    break;
            }
            tvProductName.setText(item.getName());
//            tvProductCategory.setText(item.getCategory());
//            tvProductRating.setText(String.valueOf(item.getRate()));

            DecimalFormat formatter = new DecimalFormat("#,###");
            String formattedPrice = formatter.format(item.getPrice()) + "đ";
            tvProductPrice.setText(formattedPrice);

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
            loadPreviewImage(item.getImage(), ivProductImage);
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

    private void onProductItemClick(ProductItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

        builder.setTitle("Xác nhận");
        builder.setMessage("Bạn có chắc chắn muốn thêm " + item.getName() + " vào giỏ hàng không?");

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
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                            fabCart.setVisibility(View.VISIBLE);
                            fabCart.startAnimation(shakeAnimation);
                        })
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
//        Toast.makeText(requireContext(), "Bạn đã chọn: " + item.getName(), Toast.LENGTH_SHORT).show();
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

    private void showLoading(boolean isShow) {
        loadingPanel.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (edtSearch != null) {
            edtSearch.clearFocus();
        }
    }

    int countData = 0;

    private void checkCartData() {
        if (mAuth == null || mAuth.getCurrentUser() == null || fabCart == null || cartRef == null) {
            // Đảm bảo không gọi nếu chưa đăng nhập hoặc chưa khởi tạo
            return;
        }

        // Gỡ listener cũ nếu có để tránh trùng lặp
        if (cartValueEventListener != null) {
            cartRef.removeEventListener(cartValueEventListener);
        }

        cartValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Cart cart = snapshot.getValue(Cart.class);
                boolean hasItems = cart != null && cart.items != null && !cart.items.isEmpty();
                if (fabCart != null) {
                    countData = 0;
                    if (hasItems) {
                        List<CartItem> items = cart.items;
                        for (CartItem item : items) {
                            countData += item.getQuantity();
                        }
                        tv_countCard.setText(String.valueOf(countData));
                        if (fabCart.getVisibility() != View.VISIBLE) {
                            fabCart.setVisibility(View.VISIBLE);
                            if (shakeAnimation != null) {
                                fabCart.startAnimation(shakeAnimation);
                            }
                        }
                    } else {
                        fabCart.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Lỗi đọc RTDB (Listener): " + error.getMessage(), error.toException());
                if (fabCart != null) {
                    fabCart.setVisibility(View.GONE);
                }
            }
        };

        // Thêm listener mới để lắng nghe thay đổi thời gian thực
        cartRef.addValueEventListener(cartValueEventListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Gỡ bỏ listener khi View bị hủy để tránh rò rỉ bộ nhớ
        if (cartRef != null && cartValueEventListener != null) {
            cartRef.removeEventListener(cartValueEventListener);
            Log.d(TAG, "Đã gỡ bỏ ValueEventListener của giỏ hàng.");
        }
    }
}