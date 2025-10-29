package vn.haui.android_project.view;

import static android.content.ContentValues.TAG;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import vn.haui.android_project.R;
import vn.haui.android_project.enums.MyConstant;

public class OrderTrackingActivity extends AppCompatActivity {


    // Firebase
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference orderRef;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private LinearLayout layoutSummary, layoutDetail;
    private View mapContainer;

    // View con trong layoutDetail
    private TextView tvEstimateArrival, tvOrderId, tvStatusTag, tvStatusDescTag;
    private TextView tvDriverName, tvLicensePlate, tvDeliveryFeeValue, tvDiscountValue, tvTotalValue;
    private ImageView stepPrepared, stepPickingUp, stepDelivering, stepFinish;
    ;
    private View stepPickingUpLine, stepDeliveringLine, stepFinishLine;

    private Button btnCancelOrder, btnConfirmOrder;
    // =============================

    // View con trong layoutDetail
    private TextView tvEstimateArrivalSummary, tvOrderIdSummary, tvStatusTagSummary, tvStatusDescTagSummary;
    private TextView tvDriverNameSummary, tvLicensePlateSummary, tvTotalValueSummary;
    private ImageView stepPreparedSummary, stepPickingUpSummary, stepDeliveringSummary, stepFinishSummary;
    private View stepPickingUpSummaryLine, stepDeliveringSummaryLine, stepFinishSummaryLine;
    // =============================

    // Các SpringAnimation
    private SpringAnimation alphaSpringSummary, alphaSpringDetail;
    private SpringAnimation scaleXSpringMap, scaleYSpringMap, alphaSpringMap;
    private SpringAnimation translateSummaryY, translateDetailY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        View bottomSheet = findViewById(R.id.bottomSheet);
        layoutSummary = findViewById(R.id.layoutSummary);
        layoutDetail = findViewById(R.id.layoutDetail);
        mapContainer = findViewById(R.id.mapContainer);

        mappingLayoutSummary();

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(screenHeight / 2);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Khởi tạo SpringAnimation
        initSprings();

        // 3️⃣ Khởi tạo Firebase
        FirebaseApp.initializeApp(this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        orderRef = firebaseDatabase.getReference("orders").child("CA321457");

        // 4️⃣ Ghi dữ liệu mẫu
        writeSampleOrder();

        // 5️⃣ Đọc realtime dữ liệu
        listenOrderRealtime();


        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        layoutSummary.setVisibility(GONE);
                        layoutDetail.setVisibility(VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        layoutSummary.setVisibility(VISIBLE);
                        layoutDetail.setVisibility(GONE);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                slideOffset = Math.max(0f, Math.min(slideOffset, 1f));

                float mapScale = 1 - (0.15f * slideOffset);
                float mapAlpha = 1 - (0.2f * slideOffset);
                scaleXSpringMap.animateToFinalPosition(mapScale);
                scaleYSpringMap.animateToFinalPosition(mapScale);
                alphaSpringMap.animateToFinalPosition(mapAlpha);

                alphaSpringSummary.animateToFinalPosition(1 - slideOffset);
                translateSummaryY.animateToFinalPosition(-50 * slideOffset);

                layoutDetail.setVisibility(VISIBLE);
                alphaSpringDetail.animateToFinalPosition(slideOffset);
                translateDetailY.animateToFinalPosition(100 * (1 - slideOffset));

                if (slideOffset < 0.05f) {
                    layoutDetail.setVisibility(GONE);
                }
            }
        });

        btnConfirmOrder.setOnClickListener(v -> {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin.", Toast.LENGTH_SHORT).show();
        });
    }

    private void mappingLayoutSummary() {
        // ✅ Gắn view con bên trong layoutSummary
        tvEstimateArrival = layoutDetail.findViewById(R.id.tv_estimate_time);
        tvOrderId = layoutDetail.findViewById(R.id.tv_order_id);
        tvStatusTag = layoutDetail.findViewById(R.id.tv_status_desc_tag);
        tvStatusDescTag = layoutDetail.findViewById(R.id.tv_status_tag);

        // ✅ Gắn view con bên trong layoutDetail
        tvDriverName = layoutDetail.findViewById(R.id.tv_driver_name);
        tvLicensePlate = layoutDetail.findViewById(R.id.tv_license_plate);
        tvDeliveryFeeValue = layoutDetail.findViewById(R.id.tv_delivery_fee_value);
        tvDiscountValue = layoutDetail.findViewById(R.id.tv_discount_value);
        tvTotalValue = layoutDetail.findViewById(R.id.tv_total_value);

        stepPrepared = layoutDetail.findViewById(R.id.step_prepared);
        stepPickingUp = layoutDetail.findViewById(R.id.step_pickingUp);
        stepDelivering = layoutDetail.findViewById(R.id.step_delivering);
        stepFinish = layoutDetail.findViewById(R.id.step_finish);

        stepPickingUpLine = layoutDetail.findViewById(R.id.step_pickingUp_line);
        stepDeliveringLine = layoutDetail.findViewById(R.id.step_delivering_line);
        stepFinishLine = layoutDetail.findViewById(R.id.step_finish_line);

        btnCancelOrder = layoutDetail.findViewById(R.id.btn_cancel_order);
        btnConfirmOrder = layoutDetail.findViewById(R.id.btn_confirm_order);


        //
        tvEstimateArrivalSummary = layoutSummary.findViewById(R.id.tv_estimate_time_summary);
        tvStatusTagSummary = layoutSummary.findViewById(R.id.tv_status_tag_summary);
        tvStatusDescTagSummary = layoutSummary.findViewById(R.id.tv_status_desc_tag_summary);
        tvOrderIdSummary = layoutSummary.findViewById(R.id.tv_order_id_summary);
        tvDriverNameSummary = layoutSummary.findViewById(R.id.tv_driver_name_summary);
        tvLicensePlateSummary = layoutSummary.findViewById(R.id.tv_license_plate_summary);
        tvTotalValueSummary = layoutSummary.findViewById(R.id.tv_total_value_summary);

        stepPreparedSummary = layoutSummary.findViewById(R.id.step_prepared_summary);
        stepPickingUpSummary = layoutSummary.findViewById(R.id.step_pickingUp_summary);
        stepDeliveringSummary = layoutSummary.findViewById(R.id.step_delivering_summary);
        stepFinishSummary = layoutSummary.findViewById(R.id.step_finish_summary);

        stepPickingUpSummaryLine = layoutSummary.findViewById(R.id.step_pickingUp_summary_line);
        stepDeliveringSummaryLine = layoutSummary.findViewById(R.id.step_delivering_summary_line);
        stepFinishSummaryLine = layoutSummary.findViewById(R.id.step_finish_summary_line);

    }

    private void initSprings() {
        alphaSpringSummary = createSpring(layoutSummary, DynamicAnimation.ALPHA);
        translateSummaryY = createSpring(layoutSummary, DynamicAnimation.TRANSLATION_Y);

        alphaSpringDetail = createSpring(layoutDetail, DynamicAnimation.ALPHA);
        translateDetailY = createSpring(layoutDetail, DynamicAnimation.TRANSLATION_Y);

        scaleXSpringMap = createSpring(mapContainer, DynamicAnimation.SCALE_X);
        scaleYSpringMap = createSpring(mapContainer, DynamicAnimation.SCALE_Y);
        alphaSpringMap = createSpring(mapContainer, DynamicAnimation.ALPHA);
    }

    private SpringAnimation createSpring(View view, DynamicAnimation.ViewProperty property) {
        SpringAnimation anim = new SpringAnimation(view, property);
        SpringForce spring = new SpringForce();
        spring.setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        spring.setStiffness(SpringForce.STIFFNESS_LOW);
        anim.setSpring(spring);
        return anim;
    }

    private void displayOrderDetails() {
        // ✅ Cập nhật thông tin tổng quan
        if (tvEstimateArrival != null) tvEstimateArrival.setText("Estimate arrival: 10:10");
        if (tvOrderId != null) tvOrderId.setText("Order ID: CA321457");
        if (tvStatusTag != null) tvStatusTag.setText("Driver is picking up your Order.");

        // ✅ Cập nhật thông tin tài xế
        if (tvDriverName != null) tvDriverName.setText("Adam West");
        if (tvLicensePlate != null) tvLicensePlate.setText("34 LD 5225");

        // ✅ Cập nhật thông tin tài chính
        if (tvDeliveryFeeValue != null) tvDeliveryFeeValue.setText("$0");
        if (tvDiscountValue != null) tvDiscountValue.setText("-$15");
        if (tvTotalValue != null) tvTotalValue.setText("$115");
    }


    /**
     * Ghi đơn hàng mẫu vào Firebase
     */
    private void writeSampleOrder() {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", "CA321457");
        orderData.put("status", MyConstant.PREPARED);
        orderData.put("driver", "Adam West");
        orderData.put("licensePlate", "0981094505");
        orderData.put("deliveryFee", "$0");
        orderData.put("discount", "-$15");
        orderData.put("total", "$115");
        orderRef.setValue(orderData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Order data written successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to write order: " + e.getMessage()));
    }


    /**
     * Lắng nghe realtime thay đổi đơn hàng
     */
    private void listenOrderRealtime() {
        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String orderId = snapshot.child("orderId").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                String driver = snapshot.child("driver").getValue(String.class);
                String license = snapshot.child("licensePlate").getValue(String.class);
                String fee = snapshot.child("deliveryFee").getValue(String.class);
                String discount = snapshot.child("discount").getValue(String.class);
                String total = snapshot.child("total").getValue(String.class);
                updateOrderUI(orderId, status, driver, license, fee, discount, total);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase read failed: " + error.getMessage());
            }
        });
    }

    private void updateOrderUI(String orderId, String status, String driver,
                               String license, String fee, String discount, String total) {
        runOnUiThread(() -> {
            tvOrderId.setText("Order ID: " + orderId);
            tvStatusTag.setText(status);
            tvDriverName.setText(driver);
            tvLicensePlate.setText(license);
            tvDeliveryFeeValue.setText(fee);
            tvDiscountValue.setText(discount);
            tvTotalValue.setText(total);


            tvOrderIdSummary.setText(orderId);
            tvEstimateArrivalSummary.setText("Estimate arrival: 10:10");
            tvDriverNameSummary.setText(driver);
            tvLicensePlateSummary.setText(license);
            tvTotalValueSummary.setText(total);
            mappingStep(status);
        });
    }


    private void mappingStep(String status) {
        if (status.equals(MyConstant.PREPARED)) {


            stepPrepared.setImageResource(R.drawable.ic_prepared_order_active);
            tvStatusTag.setText(ContextCompat.getString(this, R.string.prepared));
            tvStatusDescTag.setText(ContextCompat.getString(this, R.string.preparedDesc));

            stepPreparedSummary.setImageResource(R.drawable.ic_prepared_order_active);
            tvStatusTagSummary.setText(ContextCompat.getString(this, R.string.prepared));
            tvStatusDescTagSummary.setText(ContextCompat.getString(this, R.string.preparedDesc));


        } else if (status.equals(MyConstant.PICKINGUP)) {
            btnCancelOrder.setVisibility(GONE);

            stepPickingUp.setImageResource(R.drawable.ic_picking_up_order_active);
            stepPickingUpLine.setBackgroundColor(Color.parseColor("#EB4D57"));
            tvStatusTag.setText(ContextCompat.getString(this, R.string.pickingUp));
            tvStatusDescTag.setText(ContextCompat.getString(this, R.string.pickingUpDesc));

            stepPickingUpSummary.setImageResource(R.drawable.ic_picking_up_order_active);
            stepPickingUpSummaryLine.setBackgroundColor(Color.parseColor("#EB4D57"));
            tvStatusTagSummary.setText(ContextCompat.getString(this, R.string.pickingUp));
            tvStatusDescTagSummary.setText(ContextCompat.getString(this, R.string.pickingUpDesc));
        } else if (status.equals(MyConstant.DELIVERING)) {
            btnCancelOrder.setVisibility(GONE);

            stepDelivering.setImageResource(R.drawable.ic_delivering_order_active);
            stepDeliveringLine.setBackgroundColor(Color.parseColor("#EB4D57"));
            tvStatusTag.setText(ContextCompat.getString(this, R.string.delivering));
            tvStatusDescTag.setText(ContextCompat.getString(this, R.string.deliveringDesc));


            stepDeliveringSummary.setImageResource(R.drawable.ic_delivering_order_active);
            stepDeliveringSummaryLine.setBackgroundColor(Color.parseColor("#EB4D57"));
            tvStatusTagSummary.setText(ContextCompat.getString(this, R.string.delivering));
            tvStatusDescTagSummary.setText(ContextCompat.getString(this, R.string.deliveringDesc));
        } else if (status.equals(MyConstant.FINISH)) {
            btnCancelOrder.setVisibility(GONE);
            btnConfirmOrder.setVisibility(VISIBLE);

            stepFinish.setImageResource(R.drawable.ic_finish_order_active);
            stepFinishLine.setBackgroundColor(Color.parseColor("#EB4D57"));
            tvStatusTag.setText(ContextCompat.getString(this, R.string.finish));
            tvStatusDescTag.setText(ContextCompat.getString(this, R.string.finishDesc));

            stepFinishSummary.setImageResource(R.drawable.ic_finish_order_active);
            stepFinishSummaryLine.setBackgroundColor(Color.parseColor("#EB4D57"));
            tvStatusTagSummary.setText(ContextCompat.getString(this, R.string.finish));
            tvStatusDescTagSummary.setText(ContextCompat.getString(this, R.string.finishDesc));
        }
    }
}
