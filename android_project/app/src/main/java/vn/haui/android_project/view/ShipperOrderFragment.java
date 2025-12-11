package vn.haui.android_project.view;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.ShipperOrderAdapter;
import vn.haui.android_project.enums.MyConstant;
import vn.haui.android_project.model.OrderShiperHistory;
import vn.haui.android_project.services.LocationService;

public class ShipperOrderFragment extends Fragment implements ShipperOrderAdapter.OnItemClickListener {

    // Khai báo các biến View
    private Spinner spinnerStatus;
    private RecyclerView recyclerViewOrders;
    private Toolbar toolbar;

    // Khai báo các biến dữ liệu và logic
    private String currentUserId;
    private Calendar myCalendar;
    private ShipperOrderAdapter shipperOrderAdapter;
    private List<OrderShiperHistory> listOrder = new ArrayList<>();
    private DatabaseReference mDatabase;
    private LocationService locationService; // Đã thêm vào

    public ShipperOrderFragment() {
        // Required empty public constructor
    }

    // onCreateView chỉ nên làm một việc: Inflate layout và trả về view
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shipper_order, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerStatus = view.findViewById(R.id.spinner_status_filter);
        recyclerViewOrders = view.findViewById(R.id.recycler_view_orders);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("shippers").child(currentUserId);
        myCalendar = Calendar.getInstance();
        locationService = new LocationService(requireContext()); // Sử dụng requireContext() để đảm bảo không null

        setupRecyclerView();
        initDropdown();

        //updateLabel();
        getOrderHistory();
    }


    private void setupRecyclerView() {
        shipperOrderAdapter = new ShipperOrderAdapter(requireContext(), listOrder, this);
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewOrders.setAdapter(shipperOrderAdapter);
    }



    private void initDropdown() {
        String[] filterOptions = new String[]{"Tất cả", "Đã xong", "Đang giao"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                filterOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterData();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ... các hàm getOrderHistory, filterData, updateLabel, onItemClick không thay đổi ...
    // Nhưng cần thay thế view.getContext() bằng requireContext() để an toàn hơn
    // và thay thế this.view.findViewById bằng view.findViewById trong các hàm cũ nếu có.

    @Override
    public void onItemClick(OrderShiperHistory order) {
        if (order.getStatus().contains(MyConstant.FINISH))
            return;

        Intent intent1 = new Intent(requireContext(), OrderTrackingActivity.class);
        intent1.putExtra("ORDER_ID", order.getOrderId());
        // Không cần xóa task ở đây, trừ khi bạn muốn màn hình lịch sử biến mất
        // intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent1);
    }

    private void filterData() {
        if (spinnerStatus == null || listOrder == null || listOrder.isEmpty() || shipperOrderAdapter == null) {
            return;
        }
        List<OrderShiperHistory> orderAll = new ArrayList<>();
        orderAll.addAll(listOrder);
        String selectedStatus = spinnerStatus.getSelectedItem().toString();

        List<OrderShiperHistory> filteredList = new ArrayList<>();
        for (OrderShiperHistory order : orderAll) {
            boolean statusMatch = selectedStatus.equals("Tất cả") ||
                    (order.getStatus() != null && shipperOrderAdapter.getTextStatus(order.getStatus()).equalsIgnoreCase(selectedStatus));

            if (statusMatch) {
                filteredList.add(order);
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Collections.sort(filteredList, new Comparator<OrderShiperHistory>() {
            @Override
            public int compare(OrderShiperHistory o1, OrderShiperHistory o2) {
                final String PRIORITY_STATUS = "shipping";

                boolean isO1Shipping = PRIORITY_STATUS.equals(o1.getStatus());
                boolean isO2Shipping = PRIORITY_STATUS.equals(o2.getStatus());

                if (isO1Shipping && !isO2Shipping) {
                    return -1;
                }
                if (!isO1Shipping && isO2Shipping) {
                    return 1;
                }

                try {
                    Date date1 = sdf.parse(o1.getDate());
                    Date date2 = sdf.parse(o2.getDate());

                    return date1.compareTo(date2);

                } catch (ParseException | NullPointerException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });
        shipperOrderAdapter.filterList(filteredList);

        if(filteredList.isEmpty()){
            Toast.makeText(requireContext(), "Không tìm thấy đơn hàng nào.", Toast.LENGTH_SHORT).show();
        }

    }

    private void getOrderHistory() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> defaultData = new HashMap<>();
                    defaultData.put("status", "ready");
                    defaultData.put("historys", new ArrayList<>());
                    mDatabase.setValue(defaultData);
                    return;
                }

                listOrder.clear();
                if (snapshot.hasChild("historys")) {
                    for (DataSnapshot orderSnapshot : snapshot.child("historys").getChildren()) {
                        OrderShiperHistory order = orderSnapshot.getValue(OrderShiperHistory.class);
                        if (order != null) {
                            listOrder.add(order);
                        }
                    }
                }

                filterData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}