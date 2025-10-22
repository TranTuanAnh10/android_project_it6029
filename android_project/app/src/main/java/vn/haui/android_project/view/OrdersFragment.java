package vn.haui.android_project.view;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import vn.haui.android_project.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OrdersFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OrdersFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    Button btnDeliver, btnHistory, btnBasket;

    TextView label;

    public OrdersFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OrdersFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OrdersFragment newInstance(String param1, String param2) {
        OrdersFragment fragment = new OrdersFragment();
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
        View view =  inflater.inflate(R.layout.fragment_orders, container, false);
        btnDeliver = view.findViewById(R.id.btnDeliver);
        btnHistory = view.findViewById(R.id.btnHistory);
        btnBasket = view.findViewById(R.id.btnBasket);
        label = view.findViewById(R.id.orders_fragment_label);
        // Mặc định hiển thị tab "Deliver now"
        replaceChildFragment(new OrdersActiveFragment());

        btnDeliver.setOnClickListener(v -> {
            replaceChildFragment(new OrdersActiveFragment());
            setActiveButton(btnDeliver);
            label.setText("Active orders");
        });

        btnHistory.setOnClickListener(v -> {
            replaceChildFragment(new OrdersHistoryFragment());
            setActiveButton(btnHistory);
            label.setText("All orders");

        });

        btnBasket.setOnClickListener(v -> {
            replaceChildFragment(new OrdersBasketFragment());
            setActiveButton(btnBasket);
            label.setText("In-basket items");


        });

        return view;

    }
    private void replaceChildFragment(Fragment fragment)
    {
            getChildFragmentManager().beginTransaction().replace(R.id.ordersContentFrame,fragment).commit();
    }
    private void setActiveButton(Button activeButton) {
        // Màu bạn định nghĩa trong colors.xml
        int activeColor = getResources().getColor(R.color.md_theme_primary, null);
        int inactiveColor = getResources().getColor(R.color.md_theme_surfaceVariant, null);
        int activeText = getResources().getColor(android.R.color.white, null);
        int inactiveText = getResources().getColor(R.color.md_theme_onBackground, null);

        // Reset tất cả
        btnDeliver.setBackgroundTintList(android.content.res.ColorStateList.valueOf(inactiveColor));
        btnHistory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(inactiveColor));
        btnBasket.setBackgroundTintList(android.content.res.ColorStateList.valueOf(inactiveColor));

        btnDeliver.setTextColor(inactiveText);
        btnHistory.setTextColor(inactiveText);
        btnBasket.setTextColor(inactiveText);

        // Kích hoạt nút hiện tại
        activeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeColor));
        activeButton.setTextColor(activeText);
    }
}