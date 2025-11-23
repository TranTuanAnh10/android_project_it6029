package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.OrderManagementAdapter;
import vn.haui.android_project.entity.Order;
import vn.haui.android_project.enums.MyConstant;

public class OrderManagementFragment extends Fragment {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoOrders;

    private OrderManagementAdapter adapter;
    private List<Order> allOrdersList = new ArrayList<>();
    private List<Order> displayList = new ArrayList<>();

    private DatabaseReference mDatabase;
    // Mặc định hiển thị tab đầu tiên: Chờ xác nhận
    private String currentStatus = MyConstant.PREPARED;

    public OrderManagementFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // FIX CỨNG URL ĐỂ TRÁNH LỖI KẾT NỐI
        String dbUrl = "https://haui-23d87-default-rtdb.asia-southeast1.firebasedatabase.app";
        mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("orders");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupTabs();
        loadOrdersFromFirebase();
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout_orders);
        recyclerView = view.findViewById(R.id.recycler_view_orders);
        progressBar = view.findViewById(R.id.progress_bar_orders);
        tvNoOrders = view.findViewById(R.id.tv_no_orders);
    }

    private void setupRecyclerView() {
        adapter = new OrderManagementAdapter(getContext(), displayList, order -> {
            Intent intent = new Intent(getContext(), OrderDetailManagementActivity.class);
            intent.putExtra("ORDER_ID", order.getOrderId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        // 4 TAB TƯƠNG ỨNG 4 TRẠNG THÁI
        tabLayout.addTab(tabLayout.newTab().setText("Chờ xác nhận"));  // 0: prepared
        tabLayout.addTab(tabLayout.newTab().setText("Đang lấy hàng")); // 1: pickingUp
        tabLayout.addTab(tabLayout.newTab().setText("Đang giao"));     // 2: delivering
        tabLayout.addTab(tabLayout.newTab().setText("Hoàn thành"));    // 3: finish
        tabLayout.addTab(tabLayout.newTab().setText("Từ chối"));    // 4: reject
        tabLayout.addTab(tabLayout.newTab().setText("Đã huỷ"));    // 4: cancel

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentStatus = MyConstant.PREPARED; break;
                    case 1: currentStatus = MyConstant.PICKINGUP; break;
                    case 2: currentStatus = MyConstant.DELIVERING; break;
                    case 3: currentStatus = MyConstant.FINISH; break;
                    case 4: currentStatus = MyConstant.REJECT; break;
                    case 5: currentStatus = MyConstant.CANCEL_ORDER; break;
                }
                filterListByStatus(currentStatus);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadOrdersFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allOrdersList.clear();
                List<Order> tempList = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Order order = data.getValue(Order.class);
                        if (order != null) {
                            if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
                                order.setOrderId(data.getKey());
                            }
                            tempList.add(order);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("OrderError", "Lỗi convert tại key: " + data.getKey());
                    }
                }

                allOrdersList.addAll(tempList);
                Collections.reverse(allOrdersList); // Đảo ngược để đơn mới lên đầu

                filterListByStatus(currentStatus);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi tải: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void filterListByStatus(String status) {
        displayList.clear();
        for (Order order : allOrdersList) {
            if (status.equalsIgnoreCase(order.getStatus())) {
                displayList.add(order);
            }
        }
        adapter.notifyDataSetChanged();

        if (displayList.isEmpty()) {
            tvNoOrders.setVisibility(View.VISIBLE);
            String statusVN = "";
            if(status.equals(MyConstant.PREPARED)) statusVN = "Chờ xác nhận";
            else if(status.equals(MyConstant.PICKINGUP)) statusVN = "Đang lấy hàng";
            else if(status.equals(MyConstant.DELIVERING)) statusVN = "Đang giao hàng";
            else if(status.equals(MyConstant.FINISH)) statusVN = "Hoàn thành";
            else if (status.equals(MyConstant.REJECT)) statusVN = "Từ chối";
            else if (status.equals(MyConstant.CANCEL_ORDER)) statusVN = "Đã huỷ";
            tvNoOrders.setText("Không có đơn hàng: " + statusVN);
        } else {
            tvNoOrders.setVisibility(View.GONE);
        }
    }
}
