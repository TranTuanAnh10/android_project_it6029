package vn.haui.android_project.view;

import static android.content.ContentValues.TAG;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
import java.util.Locale;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.OrderItemsAdapter;
import vn.haui.android_project.databinding.OrderDetailScreenBinding;
import vn.haui.android_project.entity.ItemOrderProduct;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.enums.MyConstant;
import vn.haui.android_project.entity.Order;

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
    private ImageView stepPrepared, stepPickingUp, stepDelivering, stepFinish;
    private TextView tvStatusTag, tvStatusDescTag;
    private View stepPickingUpLine, stepDeliveringLine, stepFinishLine;
    private Button btnCancelOrder;



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
    }

    private void mappingView() {
        tvEstimateArrival = findViewById(R.id.tv_estimate_time);
        btnCancelOrder = findViewById(R.id.btn_cancel_order);
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


    }
    private boolean isTrackingActivityLaunched = false;
    private void listenOrderRealtime() {
        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String orderId = snapshot.child("orderId").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                // check doi man tracking di giao hang
                if (MyConstant.DELIVERING.equals(status)) {
                    if (!isTrackingActivityLaunched) {
                        isTrackingActivityLaunched = true;
                        Intent intent1 = new Intent(OrderDetailsActivity.this, OrderTrackingActivity.class);
                        intent1.putExtra("ORDER_ID", orderId);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent1);
                        finish();
                        return;
                    }
                } else {
                    isTrackingActivityLaunched = false;
                }
                String driver = snapshot.child("driver").getValue(String.class);
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
        if (status.equals(MyConstant.PREPARED)) {
            stepPrepared.setImageResource(R.drawable.ic_prepared_order_active);
            tvStatusTag.setText(ContextCompat.getString(this, R.string.prepared));
            tvStatusDescTag.setText(ContextCompat.getString(this, R.string.preparedDesc));
        } else if (status.equals(MyConstant.PICKINGUP)) {
            btnCancelOrder.setVisibility(GONE);
            stepPickingUp.setImageResource(R.drawable.ic_picking_up_order_active);
            stepPickingUpLine.setBackgroundColor(Color.parseColor("#EB4D57"));
            tvStatusTag.setText(ContextCompat.getString(this, R.string.pickingUp));
            tvStatusDescTag.setText(ContextCompat.getString(this, R.string.pickingUpDesc));
        } else if (status.equals(MyConstant.DELIVERING)) {
            btnCancelOrder.setVisibility(GONE);

            stepDelivering.setImageResource(R.drawable.ic_delivering_order_active);
            stepDeliveringLine.setBackgroundColor(Color.parseColor("#EB4D57"));
            tvStatusTag.setText(ContextCompat.getString(this, R.string.delivering));
            tvStatusDescTag.setText(ContextCompat.getString(this, R.string.deliveringDesc));
        } else if (status.equals(MyConstant.FINISH)) {
            btnCancelOrder.setVisibility(GONE);
            stepFinish.setImageResource(R.drawable.ic_finish_order_active);
            stepFinishLine.setBackgroundColor(Color.parseColor("#EB4D57"));
            tvStatusTag.setText(ContextCompat.getString(this, R.string.finish));
            tvStatusDescTag.setText(ContextCompat.getString(this, R.string.finishDesc));


        }
    }

    private void setupListeners() {

        // Nút Hủy đơn hàng
        binding.btnCancelOrder.setOnClickListener(v -> {
            Log.d(TAG, "User clicked Cancel Order");
            // Thực hiện logic hủy đơn hàng (hiển thị dialog xác nhận, gọi API)
            showConfirmationDialog("Bạn có chắc muốn hủy đơn hàng này không?");
        });

        binding.btnChat.setOnClickListener(view -> {
            try {
                // Lấy số điện thoại từ TextView thông qua binding
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

    private void updateTimelineStatus(boolean isOrderReady) {
        if (isOrderReady) {
            // Thay đổi icon thành một icon khác, ví dụ: một dấu tick màu xanh
//            binding.itemTimelinePrepared.timelinePreparedIcon.setImageResource(R.drawable.ic_prepared_order);
        } else {
            // Giữ nguyên hoặc đặt một icon mặc định
//            binding.itemTimelinePrepared.timelinePreparedIcon.setImageResource(R.drawable.ic_prepared_order_active);
        }
//         binding.timelinePreparedLayout.timelinePreparedLine.setBackgroundColor(getColor(R.color.some_color));
    }

    private void showConfirmationDialog(String message) {
        // Ở đây cần triển khai DialogFragment hoặc AlertDialog tùy chỉnh.
        // Ví dụ đơn giản:
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận hủy")
                .setMessage(message)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    // Logic hủy đơn hàng
                    Toast.makeText(this, "Đơn hàng đã được yêu cầu hủy.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Không", null)
                .show();
    }
}
