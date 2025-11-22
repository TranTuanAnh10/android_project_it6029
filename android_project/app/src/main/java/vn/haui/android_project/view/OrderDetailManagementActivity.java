package vn.haui.android_project.view;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
// Sử dụng lại Adapter có sẵn của bạn (như file bạn vừa cung cấp thông tin)
import vn.haui.android_project.adapter.OrderItemsAdapter;
import vn.haui.android_project.entity.ItemOrderProduct;
import vn.haui.android_project.entity.Order;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.enums.MyConstant;

public class OrderDetailManagementActivity extends AppCompatActivity {

    private static final String TAG = "OrderDetailManagement";

    // Views
    private TextView tvOrderId, tvStatusTag, tvStatusDescTag, tvEstimateArrival;
    private TextView tvDriverName, tvLicensePlate, tvDriverInfoTitle;
    private TextView tvLocationTitle, tvAddressDetail, tvRecipientContact, tvRecipientPhone;
    private TextView tvDeliveryFee, tvDiscount, tvTotal;

    private ImageView stepPrepared, stepPickingUp, stepDelivering, stepFinish, imgLocationIcon;
    private View stepPickingUpLine, stepDeliveringLine, stepFinishLine, divider1;
    private ConstraintLayout layoutDriverDetails;

    // RecyclerView
    private RecyclerView rvOrderItems;
    private OrderItemsAdapter orderItemsAdapter;
    private List<ItemOrderProduct> productList = new ArrayList<>();

    // Actions
    private LinearLayout layoutActions;
    private Button btnConfirm;
    private Button btnCancel;

    // Firebase
    private DatabaseReference orderRef;
    private String orderId;
    private ImageView btnBack;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail_management);

        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Kết nối Firebase
        orderRef = FirebaseDatabase.getInstance().getReference(DatabaseTable.ORDERS.getValue()).child(orderId);

        initViews();
        setupRecyclerView();
        loadOrderData();
        // Dòng này sẽ chạy ngon lành sau khi initViews đã chạy
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }    }

    private void initViews() {
        // Header Info
        tvOrderId = findViewById(R.id.tv_order_info_title);
        tvStatusTag = findViewById(R.id.tv_status_tag);
        tvStatusDescTag = findViewById(R.id.tv_status_desc_tag);
        tvEstimateArrival = findViewById(R.id.tv_estimate_time);

        // Timeline Steps (Icon và Line)
        stepPrepared = findViewById(R.id.step_prepared);
        stepPickingUp = findViewById(R.id.step_pickingUp);
        stepDelivering = findViewById(R.id.step_delivering);
        stepFinish = findViewById(R.id.step_finish);
        stepPickingUpLine = findViewById(R.id.step_pickingUp_line);
        stepDeliveringLine = findViewById(R.id.step_delivering_line);
        stepFinishLine = findViewById(R.id.step_finish_line);

        // Driver Info
        tvDriverInfoTitle = findViewById(R.id.tv_driver_info_title);
        layoutDriverDetails = findViewById(R.id.layout_driver_details);
        tvDriverName = findViewById(R.id.tv_driver_name);
        tvLicensePlate = findViewById(R.id.tv_license_plate);
        divider1 = findViewById(R.id.divider_1);

        // Location Info
        tvLocationTitle = findViewById(R.id.tv_location_title);
        tvAddressDetail = findViewById(R.id.tv_address_detail);
        tvRecipientContact = findViewById(R.id.tv_recipient_contact);
        tvRecipientPhone = findViewById(R.id.tv_recipient_phone);
        imgLocationIcon = findViewById(R.id.img_location_icon);

        // Financial Info
        tvDeliveryFee = findViewById(R.id.tv_delivery_fee_value);
        tvDiscount = findViewById(R.id.tv_discount_value);
        tvTotal = findViewById(R.id.tv_total_value);

        // Buttons Actions
        layoutActions = findViewById(R.id.layout_actions);
        btnConfirm = findViewById(R.id.btn_confirm_order);
        btnCancel = findViewById(R.id.btn_cancel_order);

        rvOrderItems = findViewById(R.id.rv_order_items);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupRecyclerView() {
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        // Dùng OrderItemsAdapter có sẵn
        orderItemsAdapter = new OrderItemsAdapter(productList);
        rvOrderItems.setAdapter(orderItemsAdapter);
    }

    private void loadOrderData() {
        // Dùng addValueEventListener để tự động cập nhật UI khi Firebase thay đổi (Realtime)
        // Không cần phải load lại thủ công khi bấm confirm
        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // Lấy dữ liệu cơ bản
                String status = snapshot.child("status").getValue(String.class);
                String driver = snapshot.child("driver").getValue(String.class);
                String license = snapshot.child("licensePlate").getValue(String.class);
                String estimateArrival = snapshot.child("timeDisplay").getValue(String.class);

                Double fee = snapshot.child("deliveryFee").getValue(Double.class);
                Double discount = snapshot.child("discount").getValue(Double.class);
                Double total = snapshot.child("total").getValue(Double.class);

                // Lấy danh sách sản phẩm
                DataSnapshot productListSnapshot = snapshot.child("productList");
                GenericTypeIndicator<List<ItemOrderProduct>> t = new GenericTypeIndicator<List<ItemOrderProduct>>() {};
                if (productListSnapshot.exists()) {
                    List<ItemOrderProduct> fetchedList = productListSnapshot.getValue(t);
                    if (fetchedList != null) {
                        productList.clear();
                        productList.addAll(fetchedList);
                        orderItemsAdapter.notifyDataSetChanged();
                    }
                }

                // Lấy địa chỉ
                DataSnapshot addressSnapshot = snapshot.child("addressUser");
                if (addressSnapshot.exists()) {
                    UserLocationEntity userLocation = addressSnapshot.getValue(UserLocationEntity.class);
                    updateLocationUI(userLocation);
                }

                // Update UI
                updateOrderInfoUI(orderId, status, driver, license, fee, discount, total, estimateArrival);

                // Xử lý logic ẩn hiện nút
                setupActionButtons(status);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        });
    }

    private void updateOrderInfoUI(String orderId, String status, String driver, String license,
                                   Double fee, Double discount, Double total, String estimate) {

        tvOrderId.setText("Mã đơn: " + orderId);
        tvEstimateArrival.setText(estimate != null ? estimate : "Đang cập nhật");

        // Hiển thị tài xế (chỉ khi có dữ liệu)
        if (driver != null && !driver.isEmpty()) {
            tvDriverName.setText(driver);
            tvLicensePlate.setText(license);
            layoutDriverDetails.setVisibility(VISIBLE);
            tvDriverInfoTitle.setVisibility(VISIBLE);
            divider1.setVisibility(VISIBLE);
        } else {
            layoutDriverDetails.setVisibility(GONE);
            tvDriverInfoTitle.setVisibility(GONE);
            divider1.setVisibility(GONE);
        }

        // Hiển thị tiền
        DecimalFormat formatter = new DecimalFormat("#,###");
        tvDeliveryFee.setText(fee != null ? formatter.format(fee) + "đ" : "0đ");
        tvDiscount.setText(discount != null ? formatter.format(discount) + "đ" : "0đ");
        tvTotal.setText(total != null ? formatter.format(total) + "đ" : "0đ");

        // Mapping thanh trạng thái (Màu đỏ tích lũy)
        mappingStep(status);
    }

    private void updateLocationUI(UserLocationEntity location) {
        if (location == null) return;
        tvLocationTitle.setText(location.getLocationType());
        tvAddressDetail.setText(location.getAddress());
        tvRecipientContact.setText(location.getRecipientName());
        tvRecipientPhone.setText(location.getPhoneNumber());

        if ("Home".equals(location.getLocationType())) {
            imgLocationIcon.setImageResource(R.drawable.ic_marker_home);
        } else if ("Work".equals(location.getLocationType())) {
            imgLocationIcon.setImageResource(R.drawable.ic_marker_work);
        } else {
            imgLocationIcon.setImageResource(R.drawable.ic_marker);
        }
    }

    // --- 1. XỬ LÝ THANH TRẠNG THÁI (TÍCH LŨY MÀU ĐỎ) ---
    private void mappingStep(String status) {
        if (status == null) return;

        // Reset mặc định về màu xám hết trước khi set màu đỏ
        resetStepColors();

        // Màu đỏ dùng mã #EB4D57 (hoặc mã màu đỏ của bạn)
        int activeColor = Color.parseColor("#EB4D57");

        if (status.equals(MyConstant.REJECT)) {
            // TRƯỜNG HỢP TỪ CHỐI: KHÔNG CÓ CÁI NÀO ĐỎ HẾT
            tvStatusTag.setText("Đã hủy");
            tvStatusDescTag.setText("Đơn hàng đã bị từ chối/hủy bỏ");
            // Có thể set chữ màu đỏ cho dễ nhìn
            tvStatusTag.setTextColor(activeColor);
            return;
        }

        // Logic tích lũy: Nếu là Finish -> thì cả Prepared, PickingUp, Delivering cũng phải đỏ

        // BƯỚC 1: PREPARED (Luôn đỏ nếu không hủy)
        stepPrepared.setImageResource(R.drawable.ic_prepared_order_active);
        tvStatusTag.setText(getString(R.string.prepared));
        tvStatusDescTag.setText(getString(R.string.preparedDesc));

        if (status.equals(MyConstant.PREPARED)) return; // Dừng ở đây nếu chỉ mới prepared

        // BƯỚC 2: PICKING UP
        stepPickingUp.setImageResource(R.drawable.ic_picking_up_order_active);
        stepPickingUpLine.setBackgroundColor(activeColor);
        tvStatusTag.setText(getString(R.string.pickingUp));
        tvStatusDescTag.setText(getString(R.string.pickingUpDesc));

        if (status.equals(MyConstant.PICKINGUP)) return;

        // BƯỚC 3: DELIVERING
        stepDelivering.setImageResource(R.drawable.ic_delivering_order_active);
        stepDeliveringLine.setBackgroundColor(activeColor);
        tvStatusTag.setText(getString(R.string.delivering));
        tvStatusDescTag.setText(getString(R.string.deliveringDesc));

        if (status.equals(MyConstant.DELIVERING)) return;

        // BƯỚC 4: FINISH
        if (status.equals(MyConstant.FINISH)) {
            stepFinish.setImageResource(R.drawable.ic_finish_order_active);
            stepFinishLine.setBackgroundColor(activeColor);
            tvStatusTag.setText(getString(R.string.finish));
            tvStatusDescTag.setText(getString(R.string.finishDesc));
        }
    }

    private void resetStepColors() {
        // Đặt lại icon xám (cần file drawable _inactive tương ứng của bạn, hoặc dùng icon mặc định)
        // Ở đây giả sử bạn chỉ muốn set logic đỏ, còn logic xám thì giữ nguyên layout ban đầu
        // Nếu cần thiết, bạn set lại resource ở đây.
        // Ví dụ: stepPickingUpLine.setBackgroundColor(Color.parseColor("#E0E0E0"));
    }

    // --- 2. XỬ LÝ NÚT BẤM ---
    private void setupActionButtons(String status) {
        if (status == null) return;

        // Chỉ hiện nút khi ở trạng thái CHỜ XÁC NHẬN
        if (status.equalsIgnoreCase(MyConstant.PREPARED)) {
            layoutActions.setVisibility(VISIBLE);

            btnConfirm.setText("Xác nhận đơn hàng");
            btnCancel.setText("Từ chối đơn hàng");

            btnConfirm.setOnClickListener(v -> updateOrderStatus(MyConstant.PICKINGUP));
            btnCancel.setOnClickListener(v -> updateOrderStatus(MyConstant.REJECT));
        } else {
            // Các trạng thái khác (PickingUp, Delivering, Finish, Reject) -> ẨN HẾT
            layoutActions.setVisibility(GONE);
        }
    }

    private void updateOrderStatus(String newStatus) {
        // Cập nhật trạng thái lên Firebase
        orderRef.child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    String msg = "Đã xác nhận đơn hàng!";
                    if(newStatus.equals(MyConstant.REJECT)) msg = "Đã từ chối đơn hàng!";

                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

                    // Vì ta dùng addValueEventListener (Realtime) ở hàm loadOrderData
                    // Nên khi Firebase thay đổi status -> UI sẽ tự động cập nhật lại (Ẩn nút, Đổi màu step bar)
                    // Không cần reload thủ công hay finish() trừ khi bạn muốn thoát.
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
