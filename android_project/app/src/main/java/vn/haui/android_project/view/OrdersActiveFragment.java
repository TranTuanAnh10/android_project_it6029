package vn.haui.android_project.view;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.Order;
import vn.haui.android_project.enums.MyConstant;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OrdersActiveFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OrdersActiveFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    List<Order> orders = new ArrayList<>();

    public OrdersActiveFragment() {
        // Required empty public constructor
    }
    private RecyclerView recyclerView;

    private FirebaseAuth mAuth;

    private ProgressBar progressBar;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OrdersActiveFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OrdersActiveFragment newInstance(String param1, String param2) {
        OrdersActiveFragment fragment = new OrdersActiveFragment();
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

    private void loadActiveOrders(List<Order> orderList) {
        progressBar.setVisibility(VISIBLE);

        if (mAuth == null || mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        DatabaseReference orderRef = FirebaseDatabase.getInstance()
                .getReference("orders");

        orderRef.get().addOnSuccessListener(snapshot -> {

            for (DataSnapshot child : snapshot.getChildren()) {
                Order order = child.getValue(Order.class);
                if (order != null && !Objects.equals(order.getStatus(), MyConstant.FINISH) && userId.equals(order.getUid())) {
                    orders.add(order);
                }
            }

            Log.d("ORDER_DEBUG", "Orders: " + new Gson().toJson(orders));
            // Gắn adapter
            OrderAdapter adapter = new OrderAdapter(orders);
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(VISIBLE);
            progressBar.setVisibility(GONE);
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_orders_active, container, false);
        recyclerView = view.findViewById(R.id.rvActiveOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        progressBar = view.findViewById(R.id.progressBar);
        recyclerView.setVisibility(GONE);
        progressBar.setVisibility(VISIBLE);
        // Lấy dữ liệu
        loadActiveOrders(this.orders);

        Log.d("NUMBERS_ORDER", "Numbers of orders: " + new Gson().toJson(orders.size()));
        return view;
    }
}