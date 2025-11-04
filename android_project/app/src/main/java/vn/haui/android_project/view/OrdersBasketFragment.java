package vn.haui.android_project.view;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.Cart;
import vn.haui.android_project.entity.CartItem;
import vn.haui.android_project.entity.FoodItem;
import vn.haui.android_project.entity.ProductItem;
import vn.haui.android_project.entity.Store;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OrdersBasketFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OrdersBasketFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private FirebaseAuth mAuth;

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
    View view;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_orders_basket, container, false);
        this.view = view;

        rvBasketStores = view.findViewById(R.id.rvBasketOrders);

        loadCart();

        return view;
    }
    private void loadCart() {
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

                updateCart(cart);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Lỗi đọc RTDB", error.toException());
                Toast.makeText(view.getContext(), "Lỗi đọc RTDB " + error.toException(), Toast.LENGTH_LONG).show();
            }
        });
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
            tvQuantity.setText(cartItem.getQuantity());
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


            parent.addView(itemView);
        }
    }

}