package vn.haui.android_project.view;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import vn.haui.android_project.OnTaskCompleted;
import vn.haui.android_project.R;
import vn.haui.android_project.entity.Cart;
import vn.haui.android_project.entity.CartItem;
import vn.haui.android_project.entity.FoodItem;
import vn.haui.android_project.entity.ProductItem;
import vn.haui.android_project.entity.Store;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OrdersBasketFragment#newInstance} factory method to
 * create an instance of this fragment. */
public class OrdersBasketFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private FirebaseAuth mAuth;

    private Button btnOrder;

    private ProgressBar progressBar;

    public OrdersBasketFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OrdersBasketFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OrdersBasketFragment newInstance(String param1, String param2) {
        OrdersBasketFragment fragment = new OrdersBasketFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mAuth = FirebaseAuth.getInstance();
    }

    LinearLayout rvBasketStores;
    List<Store> storeList;
    BasketStoreAdapter adapter;

    private Cart cartData;
    View view;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_orders_basket, container, false);
        this.view = view;
        progressBar = view.findViewById(R.id.progressBar);
        rvBasketStores = view.findViewById(R.id.rvBasketOrders);
        btnOrder = view.findViewById(R.id.btn_order_cart);
        btnOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getActivity();
                if (context == null) {
                    return;
                }
                syncLocalCartToFirebase(new OnTaskCompleted() {
                    @Override
                    public void onSuccess() {
                        Intent intent = new Intent(context, ConfirmPaymentActivity.class);
                        startActivity(intent);
                    }
                    @Override
                    public void onError(String errorMessage) {
                        //Toast.makeText(getContext(), "Lỗi lưu giỏ hàng: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
        loadCart();

        return view;
    }
    private void loadCart() {
        progressBar.setVisibility(View.VISIBLE); // ⬅️ Hiện loading

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
                cartData = cart;
                progressBar.setVisibility(View.GONE);
                updateCart(cartData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Lỗi đọc RTDB", error.toException());
                Toast.makeText(view.getContext(), "Lỗi đọc RTDB " + error.toException(), Toast.LENGTH_LONG).show();
            }
        });
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
    private void updateCart(Cart cart){
        LinearLayout parent = rvBasketStores;
        parent.removeAllViews();
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        for (CartItem cartItem : cart.items) {
            ProductItem item = cartItem.item_details;
            View itemView = inflater.inflate(R.layout.item_product_cart, parent, false);

            ImageView ivProductImage = itemView.findViewById(R.id.rfl_image);
            TextView tvProductDesc = itemView.findViewById(R.id.rfl_desc);
            TextView tvProductName = itemView.findViewById(R.id.rfl_name);
            TextView tvProductPrice = itemView.findViewById(R.id.rfl_price);
            TextView tvQuantity = itemView.findViewById(R.id.cart_quantity);
            TextView btnMinus = itemView.findViewById(R.id.btn_cart_minus);
            TextView btnPlus = itemView.findViewById(R.id.btn_cart_plus);

            tvProductName.setText(item.getName());
            tvProductDesc.setText(item.getDescription());
            tvQuantity.setText(cartItem.quantityToString());
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
            btnMinus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CartItem itemNew = updateLocalQuantity(item.getId(), -1);
                    if (itemNew.getQuantity() > 0){
                        tvQuantity.setText(itemNew.quantityToString());
                    }
                    else{
                        parent.removeView(itemView);
                    }
                }
            });
            btnPlus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CartItem itemNew = updateLocalQuantity(item.getId(), 1);
                    if (itemNew.getQuantity() > 0){
                        tvQuantity.setText(itemNew.quantityToString());
                    }
                    else{
                        parent.removeView(itemView);
                    }
                }
            });

            parent.addView(itemView);
        }
    }

    public CartItem updateLocalQuantity(String productId, int change) {
        if (cartData == null || cartData.items == null) return null;

        Iterator<CartItem> iterator = cartData.items.iterator();
        CartItem item = null;
        while (iterator.hasNext()) {
            item = iterator.next();
            if (item.item_details.getId().equals(productId)) {
                item.quantity = (item.getQuantity() + change);

                if (item.getQuantity() <= 0) {
                    iterator.remove();
                }
                break;
            }
        }
        return item;
    }
    @Override
    public void onStop() {
        super.onStop();
        syncLocalCartToFirebase(new OnTaskCompleted() {
            @Override
            public void onSuccess() {
                //Toast.makeText(getContext(), "Đã lưu giỏ hàng", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String errorMessage) {
                //Toast.makeText(getContext(), "Lỗi lưu giỏ hàng: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncLocalCartToFirebase(OnTaskCompleted callback) {
        if (mAuth == null || mAuth.getCurrentUser() == null) {
            return;
        }
        Cart cartToSave = cartData;

        if (cartToSave == null || cartToSave.items == null) {
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference cartRef = FirebaseDatabase.getInstance()
                .getReference("carts")
                .child(userId);

        cartRef.setValue(cartToSave)
                .addOnSuccessListener(aVoid ->
                        callback.onSuccess())
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }
}

