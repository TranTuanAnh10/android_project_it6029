package vn.haui.android_project.view;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.ShipperOrderAdapter;
import vn.haui.android_project.entity.ProductItem;
import vn.haui.android_project.enums.MyConstant;
import vn.haui.android_project.model.OrderShiperHistory;
import vn.haui.android_project.services.LocationService;

public class ShipperActivity extends AppCompatActivity implements ShipperOrderAdapter.OnItemClickListener {
    private Spinner spinnerStatus;
    private String currentUserId;
    private Button btnDatePicker;
    private Calendar myCalendar;
    private AlertDialog currentOrderDialog;
    private RecyclerView recyclerViewOrders;
    private ShipperOrderAdapter shipperOrderAdapter;
    private List<OrderShiperHistory> listOrder = new ArrayList<>();
    private DatabaseReference notiRef;
    private LocationService locationService;
    private DatabaseReference mDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shipper);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initDropdown();
        locationService = new LocationService(this);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("shippers").child(currentUserId);
        myCalendar = Calendar.getInstance();
        recyclerViewOrders = findViewById(R.id.recycler_view_orders);
        shipperOrderAdapter = new ShipperOrderAdapter(this, listOrder, this);
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOrders.setAdapter(shipperOrderAdapter);
        btnDatePicker = findViewById(R.id.btn_date_picker);

        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                updateLabel();

                // TODO: Lọc RecyclerView theo ngày
            }
        };
        btnDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(
                        ShipperActivity.this,
                        dateSetListener,
                        myCalendar.get(Calendar.YEAR),
                        myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)
                ).show();
            }
        });
        updateLabel();
        getOrderHistory();
    }
    @Override
    public void onItemClick(OrderShiperHistory order) {
        Toast.makeText(this, "Bạn đã chọn đơn hàng của: " + order.getReceiverName(), Toast.LENGTH_SHORT).show();

        if (order.getStatus().contains("done"))
            return;
         
        // và gửi kèm ID hoặc toàn bộ đối tượng đơn hàng.
        /*
        Intent intent = new Intent(ShipperActivity.this, OrderDetailActivity.class);

        // Cách 1: Gửi ID (khuyến khích nếu bạn cần lấy dữ liệu mới nhất từ Firebase ở màn hình sau)
        intent.putExtra("ORDER_ID", order.getOrderId());

        // Cách 2: Gửi toàn bộ đối tượng (để làm được điều này, lớp OrderShiperHistory
        // cần implement Serializable hoặc Parcelable)
        // intent.putExtra("ORDER_OBJECT", order);

        startActivity(intent);
        */
    }

    private void getOrderHistory(){
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> defaultData = new HashMap<>();
                    defaultData.put("status", "ready");
                    String myFormat = "dd/MM/yyyy";
                    SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
//                    listOrder.add(new OrderShiperHistory("20251118153213-787", "shipping",
//                            "Thu Thu Shop", "0987654321",
//                            "Trường Đại học Công nghiệp Hà Nội, 298, Đường Cầu Diễn, Hanoi, 34000, Vietnam",
//                            "120005", sdf.format(myCalendar.getTime())));
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

                if (snapshot.hasChild("status")) {
                    String status = snapshot.child("status").getValue(String.class);
                }

                filterData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ShipperActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForNewOrders() {
        notiRef = FirebaseDatabase.getInstance().getReference("Notifications").child(currentUserId);

        notiRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String content = snapshot.child("content").getValue(String.class);
                    String orderId = snapshot.child("orderId").getValue(String.class);
                    String notiKey = snapshot.getKey(); // Key để xóa thông báo sau này

                    // Hiển thị Dialog
                    showNewOrderDialog(title, content, orderId, notiKey);
                }
            }

            // Các hàm override khác không dùng đến nhưng bắt buộc phải để
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String s) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String s) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * 2. Hiển thị Dialog xác nhận nhận đơn
     */
    private void showNewOrderDialog(String title, String content, String orderId, String notiKey) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setCancelable(false) // Bắt buộc phải chọn
                .setPositiveButton("NHẬN ĐƠN", (dialog, which) -> {
                    // Xử lý nhận đơn (Quan trọng)
                    processOrderAcceptance(orderId, notiKey);
                })
                .setNegativeButton("ĐỂ SAU", (dialog, which) -> {
                    // Xóa thông báo khỏi máy mình để không hiện lại
                    notiRef.child(notiKey).removeValue();
                    dialog.dismiss();
                })
                .show();
    }

    private void processOrderAcceptance(String orderId, String notiKey) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);

        orderRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                String status = currentData.child("status").getValue(String.class);

                if (status != null && status.equals("picking_up") && !currentData.hasChild("shipperId")) {
                    MutableData shipperNode = currentData.child("shipper");
                    locationService.getCurrentLocation(new LocationService.LocationCallbackListener() {
                        @Override
                        public void onLocationResult(double la, double log, String add) {
                            shipperNode.child("lat").setValue(la);
                            shipperNode.child("lng").setValue(log);
                        }
                        @Override
                        public void onLocationError(String errorMessage) {
                            Toast.makeText(ShipperActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });

                    shipperNode.child("shipperId").setValue(currentUserId);

                    currentData.child("status").setValue(MyConstant.DELIVERING);

                    return Transaction.success(currentData);
                } else {
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

                notiRef.child(notiKey).removeValue();

                if (committed) {
                    Toast.makeText(ShipperActivity.this, "Nhận đơn thành công!", Toast.LENGTH_SHORT).show();

                    FirebaseDatabase.getInstance().getReference("shippers")
                            .child(currentUserId).child("status").setValue("busy");

                    saveToShipperHistory(currentData, orderId);
                    // 2. Chuyển sang màn hình chi tiết đơn hàng
                    // Intent intent = new Intent(ShipperMainActivity.this, OrderDetailActivity.class);
                    // intent.putExtra("ORDER_ID", orderId);
                    // startActivity(intent);

                } else {
                    Toast.makeText(ShipperActivity.this, "Đơn đã có người nhận.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    private void saveToShipperHistory(DataSnapshot orderSnapshot, String orderId) {
        String price = String.valueOf(orderSnapshot.child("total").getValue());
        if (price.equals("null")) price = "0";

        DataSnapshot addressNode = orderSnapshot.child("addressUser");

        String name = addressNode.child("recipientName").getValue(String.class);
        String phone = addressNode.child("phoneNumber").getValue(String.class);
        String address = addressNode.child("address").getValue(String.class);
        String currentDate = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());

        OrderShiperHistory historyItem = new OrderShiperHistory(
                orderId,
                "shipping",
                name,
                phone,
                address,
                price,
                currentDate
        );

        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference("shippers")
                .child(currentUserId)
                .child("historys");

        historyRef.child(orderId).setValue(historyItem);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notiRef != null) {

        }
    }


    private void filterData() {
        String selectedStatus = spinnerStatus.getSelectedItem().toString();
        String selectedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(myCalendar.getTime());

        List<OrderShiperHistory> filteredList = new ArrayList<>();
        for (OrderShiperHistory order : listOrder) {
            boolean statusMatch = false;
            if (selectedStatus.equals("Tất cả")) {
                statusMatch = true;
            } else {
                if(order.getStatus() != null && order.getStatus().equalsIgnoreCase(selectedStatus)){
                    statusMatch = true;
                }
            }

            boolean dateMatch = (order.getDate() != null && order.getDate().equals(selectedDate));

            if (statusMatch && dateMatch) {
                filteredList.add(order);
            }
        }

        shipperOrderAdapter.filterList(filteredList);

        if(filteredList.isEmpty()){
            Toast.makeText(this, "Không tìm thấy đơn hàng nào.", Toast.LENGTH_SHORT).show();
        }
    }



    private void updateLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());

        btnDatePicker.setText(sdf.format(myCalendar.getTime()));
    }

    private void initDropdown(){
        spinnerStatus = findViewById(R.id.spinner_status_filter);

        String[] filterOptions = new String[]{"Tất cả", "Đã giao", "Đang giao", "Không thành công"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                filterOptions
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerStatus.setAdapter(adapter);

        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = filterOptions[position];

                // TODO: Gọi hàm lọc RecyclerView của bạn tại đây
                // Ví dụ: filterOrderList(selectedStatus);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

}