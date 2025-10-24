package vn.haui.android_project.view;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.FoodItem;
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
    }

    RecyclerView rvBasketStores;
    List<Store> storeList;
    BasketStoreAdapter adapter;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_orders_basket, container, false);


        rvBasketStores = view.findViewById(R.id.rvBasketOrders);
        rvBasketStores.setLayoutManager(new LinearLayoutManager(getContext()));

        storeList = new ArrayList<>();

        // tạo dữ liệu giả
        List<FoodItem> pizzaList1 = new ArrayList<>();
        pizzaList1.add(new FoodItem("Pizza Margherita", "Large size, extra Grated Parmesan", "$70", R.drawable.img_pizza, 2));
        pizzaList1.add(new FoodItem("Pizza Pepperoni", "Large size, no extras", "$60", R.drawable.img_pizza_thap_cam, 2));

        storeList.add(new Store("Pizzeria da Giuseppe", R.drawable.img_pizza_thap_cam, pizzaList1, true));

        List<FoodItem> pizzaList2 = new ArrayList<>();
        pizzaList2.add(new FoodItem("Pizza Hải sản", "Seafood, cheese crust", "$85", R.drawable.img_pizza_thap_cam, 1));
        pizzaList2.add(new FoodItem("Pizza BBQ", "BBQ sauce, extra cheese", "$75", R.drawable.img_pizza, 3));

        storeList.add(new Store("Pizza Roma", R.drawable.img_pizza, pizzaList2, true));

        adapter = new BasketStoreAdapter(getContext(), storeList);

        Log.d("TEST", "Store count: " + storeList.size());

        rvBasketStores.setAdapter(adapter);

        return view;    }
}