package vn.haui.android_project.view;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.Order;

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

    public OrdersActiveFragment() {
        // Required empty public constructor
    }
    private RecyclerView recyclerView;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_orders_active, container, false);
        recyclerView = view.findViewById(R.id.rvActiveOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 🧩 Dữ liệu mẫu
        List<Order> orders = new ArrayList<>();
//        orders.add(new Order(
//                "The Daily Grind Hub",
//                "1 item",
//                "Estimate arrival: 10:25",
//                "Order placed",
//                "$20",
//                R.drawable.image_pizza
//        ));
//        orders.add(new Order(
//                "CFK",
//                "1 item",
//                "Estimate arrival: 10:25",
//                "Order placed",
//                "$20",
//                R.drawable.img_pizza_ga_nuong_bbq
//        ));

        // Gắn adapter
        OrderAdapter adapter = new OrderAdapter(orders);
        recyclerView.setAdapter(adapter);

        return view;
    }
}