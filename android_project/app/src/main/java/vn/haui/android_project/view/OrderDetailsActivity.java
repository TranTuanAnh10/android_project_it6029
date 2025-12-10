package vn.haui.android_project.view;

import static android.view.View.GONE;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.ActivityRating;
import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;
import vn.haui.android_project.adapter.OrderItemsAdapter;
import vn.haui.android_project.databinding.OrderDetailScreenBinding;
import vn.haui.android_project.entity.ItemOrderProduct;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.enums.MyConstant;

public class OrderDetailsActivity extends AppCompatActivity {

    private OrderDetailScreenBinding binding;
    private LinearLayout layoutTimeline;
    private static final String TAG = "OrderDetailsActivity";
    private String orderId;
    private DatabaseReference orderRef;
    private FirebaseDatabase firebaseDatabase;
    private TextView tvEstimateArrival;
    private List<ItemOrderProduct> productList = new ArrayList<>();
    private OrderItemsAdapter orderItemsAdapter;
    private RecyclerView rvOrderItems;
    private ImageView stepPrepared, stepPickingUp, stepDelivering, stepFinish, imgLocationIcon;
    private TextView tvStatusTag, tvStatusDescTag, tvDriverInfoTitle, tvLocationTitle, tvAddressDetail, tvRecipientContact, tvRecipientPhone;
    private View stepPickingUpLine, stepDeliveringLine, stepFinishLine;
    private Button btnCancelOrder, btnRateOrder;
    private ConstraintLayout layoutDriverDetails;
    private View divider1;
    private ImageButton imageButton;

    private boolean rating = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = OrderDetailScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        layoutTimeline = binding.layoutTimeline;
        rvOrderItems = findViewById(R.id.rv_order_items);
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        orderItemsAdapter = new OrderItemsAdapter(productList);
        rvOrderItems.setAdapter(orderItemsAdapter);
        imageButton = binding.btnBack;
        setupListeners();
        Intent intent = getIntent();
        if (intent != null) {
            orderId = intent.getStringExtra("ORDER_ID");
        }
        mappingView();

        // 3️⃣ Khởi tạo Firebase
        FirebaseApp.initializeApp(this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        orderRef = firebaseDatabase.getReference(DatabaseTable.ORDERS.getValue()).child(orderId);
        listenOrderRealtime();

        imageButton.setOnClickListener(v -> {
            if (isTaskRoot() && !isFinishing()) {
                Intent mainIntent = new Intent(OrderDetailsActivity.this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(mainIntent);
            }
            finish();
        });
    }

    private void mappingView() {
        tvEstimateArrival = findViewById(R.id.tv_estimate_time);
        btnCancelOrder = findViewById(R.id.btn_cancel_order);
        btnRateOrder = findViewById(R.id.btn_rate_order);
        // ✅ Gắn view con bên trong layoutSummary)
        stepPrepared = findViewById(R.id.step_prepared);
        stepPickingUp = findViewById(R.id.step_pickingUp);
        stepDelivering = findViewById(R.id.step_delivering);
        stepFinish = findViewById(R.id.step_finish);
        tvStatusTag = findViewById(R.id.tv_status_tag);
        tvStatusDescTag = findViewById(R.id.tv_status_desc_tag);
        stepPickingUpLine = findViewById(R.id.step_pickingUp_line);
        stepDeliveringLine = findViewById(R.id.step_delivering_line);
        stepFinishLine = findViewById(R.id.step_finish_line);
        tvDriverInfoTitle = findViewById(R.id.tv_driver_info_title);
        layoutDriverDetails = findViewById(R.id.layout_driver_details);
        divider1 = findViewById(R.id.divider_1);

        tvLocationTitle = findViewById(R.id.tv_location_title);
        tvAddressDetail = findViewById(R.id.tv_address_detail);
        tvRecipientContact = findViewById(R.id.tv_recipient_contact);
        tvRecipientPhone = findViewById(R.id.tv_recipient_phone);
        imgLocationIcon = findViewById(R.id.img_location_icon);
    }

    private boolean isTrackingActivityLaunched = false;

    private void listenOrderRealtime() {
        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String orderId = snapshot.child("orderId").getValue(String.class);
                DataSnapshot ratings = snapshot.child("ratings");
                if (ratings.exists()) {
                    rating = false;
                    btnRateOrder.setVisibility(View.GONE);
                }
                String status = snapshot.child("status").getValue(String.class);
                // check doi man tracking di giao hang
                if (MyConstant.DELIVERING.equals(status)) {
                    if (!isTrackingActivityLaunched) {
                        isTrackingActivityLaunched = true;
                        Intent intent1 = new Intent(OrderDetailsActivity.this, OrderTrackingActivity.class);
                        intent1.putExtra("ORDER_ID", orderId);
                        // ❌ FIX QUAN TRỌNG: Loại bỏ FLAG_ACTIVITY_CLEAR_TASK
                        // Flag này xóa toàn bộ Stack, dẫn đến việc nút Back bị lỗi thoát ứng dụng.
                        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent1);
                        finish();
                        return;
                    }
                } else {
                    isTrackingActivityLaunched = false;
                    tvDriverInfoTitle.setVisibility(GONE);
                    layoutDriverDetails.setVisibility(GONE);
                    divider1.setVisibility(GONE);
                }
                String driver = null;
//                DataSnapshot shipperSnap = snapshot.child("shipper");
//                Map<String, Object> shipperInfoMap = shipperSnap.child("shipperInfo").getValue(Map.class);
//                String driver= shipperInfoMap.get("shipperName").toString();
//                String shipperPhone = shipperInfoMap.get("shipperPhone").toString();
//                String shipperAvatar = shipperInfoMap.get("shipperAvatar").toString();

                String estimateArrival = snapshot.child("timeDisplay").getValue(String.class);
                tvEstimateArrival.setText(estimateArrival);
                String license = snapshot.child("licensePlate").getValue(String.class);
                String fee = String.valueOf(snapshot.child("deliveryFee").getValue(Double.class));
                String discount = String.valueOf(snapshot.child("discount").getValue(Double.class));
                String total = String.valueOf(snapshot.child("total").getValue(Double.class));
                DataSnapshot productListSnapshot = snapshot.child("productList");
                GenericTypeIndicator<List<ItemOrderProduct>> t = new GenericTypeIndicator<List<ItemOrderProduct>>() {
                };
                if (productListSnapshot.exists()) {
                    List<ItemOrderProduct> fetchedList = productListSnapshot.getValue(t);
                    if (fetchedList != null) {
                        productList.clear();
                        productList.addAll(fetchedList);

                    }
                }
                DataSnapshot addressUserSnapshot = snapshot.child("addressUser");
                if (addressUserSnapshot.exists()) {
                    UserLocationEntity fetchedUserLocation = addressUserSnapshot.getValue(UserLocationEntity.class);
                    mappingLocation(fetchedUserLocation);
                }
                orderItemsAdapter.notifyDataSetChanged();
                // ✅ Cập nhật UI đơn hàng
                updateOrderUI(orderId, status, driver, license, fee, discount, total, estimateArrival);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase read failed: " + error.getMessage());
            }
        });
    }

    private void updateOrderUI(String orderId, String status, String driver,
                               String license, String fee, String discount, String total, String estimate) {
        runOnUiThread(() -> {
            DecimalFormat formatter = new DecimalFormat("#,###");
            binding.tvOrderId.setText(orderId);
            binding.tvStatusTag.setText(status);
            binding.tvDriverName.setText(driver);
            binding.tvLicensePlate.setText(license);
            binding.tvDeliveryFeeValue.setText(formatter.format(Double.parseDouble(fee)) + "đ");
            binding.tvDiscountValue.setText(!discount.equals("") ? formatter.format(Double.parseDouble(discount)) + "đ" : "");
            binding.tvTotalValue.setText(formatter.format(Double.parseDouble(total)) + "đ");


            binding.tvOrderId.setText(orderId);
            binding.tvEstimateTime.setText(estimate);
            binding.tvDriverName.setText(driver);
            binding.tvLicensePlate.setText(license);
            mappingStep(status);
        });
    }

    private void mappingStep(String status) {
        final int ACTIVE_COLOR = Color.parseColor("#EB4D57"); // Màu đỏ active
        if (status.equals(MyConstant.PREPARED)) {
            stepPrepared.setImageResource(R.drawable.ic_prepared_order_active);
            tvStatusTag.setText(R.string.prepared);
            tvStatusDescTag.setText(R.string.preparedDesc);
            btnRateOrder.setVisibility(View.GONE);
        } else if (status.equals(MyConstant.PICKINGUP)) {
            stepPrepared.setImageResource(R.drawable.ic_prepared_order_active);
            stepPickingUp.setImageResource(R.drawable.ic_picking_up_order_active);
            stepPickingUpLine.setBackgroundColor(ACTIVE_COLOR);
            tvStatusTag.setText(R.string.pickingUp);
            tvStatusDescTag.setText(R.string.pickingUpDesc);
            btnCancelOrder.setVisibility(View.GONE);
            btnRateOrder.setVisibility(View.GONE);
        } else if (status.equals(MyConstant.DELIVERING)) {
            stepPrepared.setImageResource(R.drawable.ic_prepared_order_active);
            stepPickingUp.setImageResource(R.drawable.ic_picking_up_order_active);
            stepPickingUpLine.setBackgroundColor(ACTIVE_COLOR);
            stepDelivering.setImageResource(R.drawable.ic_delivering_order_active);
            stepDeliveringLine.setBackgroundColor(ACTIVE_COLOR);
            tvStatusTag.setText(R.string.delivering);
            tvStatusDescTag.setText(R.string.deliveringDesc);
            btnCancelOrder.setVisibility(View.GONE);
            btnRateOrder.setVisibility(View.GONE);
        } else if (status.equals(MyConstant.FINISH)) {
            stepPrepared.setImageResource(R.drawable.ic_prepared_order_active);
            stepPickingUp.setImageResource(R.drawable.ic_picking_up_order_active);
            stepPickingUpLine.setBackgroundColor(ACTIVE_COLOR);
            stepDelivering.setImageResource(R.drawable.ic_delivering_order_active);
            stepDeliveringLine.setBackgroundColor(ACTIVE_COLOR);
            stepFinish.setImageResource(R.drawable.ic_finish_order_active);
            stepFinishLine.setBackgroundColor(ACTIVE_COLOR);
            tvStatusTag.setText(R.string.finish);
            tvStatusDescTag.setText(R.string.finishDesc);
            btnCancelOrder.setVisibility(View.GONE);
            if (rating) {
                btnRateOrder.setVisibility(View.VISIBLE);
            }
        } else if (status.equals(MyConstant.REJECT)) {
            tvStatusTag.setText(R.string.reject_status);
            tvStatusDescTag.setText(R.string.reject_status_desc);
            btnCancelOrder.setVisibility(View.GONE);
            btnRateOrder.setVisibility(View.GONE);
        } else if (status.equals(MyConstant.CANCEL_ORDER)) {
            tvStatusTag.setText(R.string.reject_status);
            tvStatusDescTag.setText(R.string.cancel_order_status_desc);
            btnCancelOrder.setVisibility(View.GONE);
            btnRateOrder.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {

        // Nút Hủy đơn hàng
        binding.btnCancelOrder.setOnClickListener(v -> {
            Log.d(TAG, "User clicked Cancel Order");
            // Thực hiện logic hủy đơn hàng (hiển thị dialog xác nhận, gọi API)
            showConfirmationDialog("Bạn có chắc muốn hủy đơn hàng này không?");
        });
        binding.btnRateOrder.setOnClickListener(v -> {
            Intent intent1 = new Intent(OrderDetailsActivity.this, ActivityRating.class);
            intent1.putExtra("ORDER_ID", orderId);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent1);

        });
        binding.btnChat.setOnClickListener(view -> {
            try {
                // Lấy số điện thoại từ TextView thông qua binding
                // Chú ý: tvLicensePlate hiển thị biển số xe, không phải SĐT.
                // Tôi tạm thời giữ nguyên logic của bạn, nhưng bạn nên kiểm tra lại
                // trường dữ liệu nào chứa SĐT của tài xế.
                String phoneNumber = binding.tvLicensePlate.getText().toString();
                // Tạo Intent để mở ứng dụng nhắn tin
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phoneNumber));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Không thể mở ứng dụng nhắn tin", Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnCall.setOnClickListener(view -> {
            try {
                // Lấy số điện thoại từ TextView thông qua binding
                String phoneNumber = binding.tvLicensePlate.getText().toString();
                // Tạo Intent để mở ứng dụng gọi điện
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phoneNumber));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Không thể thực hiện cuộc gọi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mappingLocation(UserLocationEntity defaultAddress) {
        if (tvLocationTitle != null) tvLocationTitle.setText(defaultAddress.getLocationType());
        if (tvAddressDetail != null) tvAddressDetail.setText(defaultAddress.getAddress());
        if (tvRecipientContact != null) tvRecipientContact.setText(defaultAddress.getPhoneNumber());
        if (tvRecipientPhone != null) tvRecipientPhone.setText(defaultAddress.getRecipientName());
        if ("Home".equals(defaultAddress.getLocationType())) {
            imgLocationIcon.setImageResource(R.drawable.ic_marker_home);
        } else if ("Work".equals(defaultAddress.getLocationType())) {
            imgLocationIcon.setImageResource(R.drawable.ic_marker_work);
        } else {
            imgLocationIcon.setImageResource(R.drawable.ic_marker);
        }
    }

    private void showConfirmationDialog(String message) {
        // Ở đây cần triển khai DialogFragment hoặc AlertDialog tùy chỉnh.
        // Ví dụ đơn giản:
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận hủy")
                .setMessage(message)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    updateOrderStatus(MyConstant.CANCEL_ORDER);
                    // Logic hủy đơn hàng
                    Toast.makeText(this, "Đơn hàng đã được yêu cầu hủy.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Không", null)
                .show();
    }

    private void updateOrderStatus(String newStatus) {
        // Cập nhật trạng thái lên Firebase
        orderRef.child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    String msg = "Đã xác nhận đơn hàng!";
                    if (newStatus.equals(MyConstant.REJECT) || newStatus.equals(MyConstant.CANCEL_ORDER))
                        msg = "Đã hủy đơn hàng!";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}