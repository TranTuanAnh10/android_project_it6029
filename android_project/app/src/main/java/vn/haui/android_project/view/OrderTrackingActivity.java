package vn.haui.android_project.view;

import static android.content.ContentValues.TAG;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.android_project.R;

import vn.haui.android_project.adapter.OrderItemsAdapter;
import vn.haui.android_project.entity.ItemOrderProduct;
import vn.haui.android_project.entity.UserEntity;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.enums.MyConstant;
import vn.haui.android_project.enums.UserRole;


public class OrderTrackingActivity extends AppCompatActivity {


    // Firebase
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference orderRef;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private LinearLayout layoutSummary, layoutDetail, layoutItem;
    private View mapContainer;
    private WebView webViewMap;

    // View con trong layoutDetail
    private TextView tvEstimateArrival, tvOrderId, tvStatusTag, tvStatusDescTag;
    private TextView tvDriverName, tvLicensePlate, tvDeliveryFeeValue, tvDiscountValue, tvTotalValue, itemDefaulCount, itemDefaul;
    private ImageView stepPrepared, stepPickingUp, stepDelivering, stepFinish, itemDefaulImg;
    ;
    private View stepPickingUpLine, stepDeliveringLine, stepFinishLine;

    private Button btnCancelOrder, btnConfirmOrder;
    // =============================

    // View con trong layoutDetail
    private TextView tvEstimateArrivalSummary, tvOrderIdSummary, tvStatusTagSummary, tvStatusDescTagSummary, tvLocationTitle, tvAddressDetail, tvRecipientContact, tvRecipientPhone;
    private TextView tvDriverNameSummary, tvLicensePlateSummary, tvTotalValueSummary;
    private ImageView stepPreparedSummary, stepPickingUpSummary, stepDeliveringSummary, stepFinishSummary, imgLocationIcon;
    private View stepPickingUpSummaryLine, stepDeliveringSummaryLine, stepFinishSummaryLine;
    // =============================

    // Các SpringAnimation
    private SpringAnimation alphaSpringSummary, alphaSpringDetail;
    private SpringAnimation scaleXSpringMap, scaleYSpringMap, alphaSpringMap;
    private SpringAnimation translateSummaryY, translateDetailY;

    private String orderId;

    private RecyclerView rvOrderItems;
    private OrderItemsAdapter orderItemsAdapter;
    private String userRole;
    private List<ItemOrderProduct> productList = new ArrayList<>();
    DecimalFormat formatter = new DecimalFormat("#,###");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        View bottomSheet = findViewById(R.id.bottomSheet);
        layoutSummary = findViewById(R.id.layoutSummary);
        layoutDetail = findViewById(R.id.layoutDetail);
        mapContainer = findViewById(R.id.mapContainer);

        rvOrderItems = findViewById(R.id.rv_order_items);
        mappingViewMap();
        mappingLayoutSummary();

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(screenHeight / 2);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Khởi tạo SpringAnimation
        initSprings();
        Intent intent = getIntent();

        if (intent != null) {
            orderId = intent.getStringExtra("ORDER_ID");
            Log.d("TAG", "onCreate: " + orderId);
            // Lấy orderId từ Intent")
        }
        // 3️⃣ Khởi tạo Firebase
        FirebaseApp.initializeApp(this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        orderRef = firebaseDatabase.getReference(DatabaseTable.ORDERS.getValue()).child(orderId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection(DatabaseTable.USERS.getValue()).document(FirebaseAuth.getInstance().getUid());
        userRef.get().addOnSuccessListener(snapshot -> {
            UserEntity userFirebase = snapshot.toObject(UserEntity.class);
            userRole = userFirebase.getRole();
            if(userRole.contains(UserRole.SHIPPER.getValue())){
                btnConfirmOrder.setVisibility(VISIBLE);
            }
        });
        // 4️⃣ Ghi dữ liệu mẫu
//        writeSampleOrder();

        // 5️⃣ Đọc realtime dữ liệu
        listenOrderRealtime();
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));

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
            //Toast.makeText(this, "Giao hàng thanh cong", Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận hoàn thành")
                    .setMessage("Bạn xác nhận đã giao đơn hàng này thành công và muốn kết thúc đơn?")
                    .setCancelable(false)
                    .setPositiveButton("Đúng, đã giao", (dialog, which) -> {
                        completeOrder(orderId);
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();

        });

    }

    private void completeOrder(String orderId) {
        String currentShipperId = FirebaseAuth.getInstance().getUid();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        Map<String, Object> updates = new HashMap<>();

        updates.put("/orders/" + orderId + "/status", MyConstant.FINISH);

        updates.put("/shippers/" + currentShipperId + "/status", "ready");

        updates.put("/shippers/" + currentShipperId + "/historys/" + orderId + "/status", MyConstant.FINISH);

        rootRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Giao hàng thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                    Intent intent1 = new Intent(OrderTrackingActivity.this, ShipperActivity.class);
                    this.startActivity(intent1);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        tvLocationTitle = layoutDetail.findViewById(R.id.tv_location_title);
        tvAddressDetail = layoutDetail.findViewById(R.id.tv_address_detail);
        tvRecipientContact = layoutDetail.findViewById(R.id.tv_recipient_contact);
        tvRecipientPhone = layoutDetail.findViewById(R.id.tv_recipient_phone);
        imgLocationIcon = layoutDetail.findViewById(R.id.img_location_icon);

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
        itemDefaulImg = layoutSummary.findViewById(R.id.item_defaul_img);
        itemDefaulCount = layoutSummary.findViewById(R.id.item_defaul_count);
        itemDefaul = layoutSummary.findViewById(R.id.item_defaul);
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


    /**
     * Lắng nghe realtime thay đổi đơn hàng (Firebase)
     */
    private void listenOrderRealtime() {
        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String orderId = snapshot.child("orderId").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                String driver = snapshot.child("driver").getValue(String.class);
                String estimateArrival = snapshot.child("timeDisplay").getValue(String.class);
                tvEstimateArrival.setText(estimateArrival);
                String license = snapshot.child("licensePlate").getValue(String.class);
                String fee = String.valueOf(snapshot.child("deliveryFee").getValue(Double.class));
                String discount = String.valueOf(snapshot.child("discount").getValue(Double.class));
                String total = String.valueOf(snapshot.child("total").getValue(Double.class));
                DataSnapshot addressUserSnapshot = snapshot.child("addressUser");
                if (addressUserSnapshot.exists()) {
                    UserLocationEntity fetchedUserLocation = addressUserSnapshot.getValue(UserLocationEntity.class);
                    mappingLocation(fetchedUserLocation);
                }
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
                if (!productList.isEmpty()) {
                    ItemOrderProduct itemOrderProduct = productList.get(0);
                    Integer quantity = itemOrderProduct.getQuantity();
                    String quantityText = quantity != null ? "x" + String.valueOf(quantity) : "x1";
                    tvTotalValueSummary.setText(formatter.format(itemOrderProduct.getTotalPrice()) + "đ");
                    itemDefaulCount.setText(quantityText);
                    itemDefaul.setText(itemOrderProduct.getName());
                    String imageName = itemOrderProduct.getImage();
                    loadPreviewImage(imageName, itemDefaulImg);
                }

                orderItemsAdapter = new OrderItemsAdapter(productList);
                rvOrderItems.setAdapter(orderItemsAdapter);
                // ✅ Cập nhật UI đơn hàng
                updateOrderUI(orderId, status, driver, license, fee, discount, total, estimateArrival);
                DataSnapshot receiverSnap = snapshot.child("receiver");
                Double receiverLat = receiverSnap.child("lat").getValue(Double.class);
                Double receiverLon = receiverSnap.child("lng").getValue(Double.class);
                DataSnapshot shipperSnap = snapshot.child("shipper");
                Double currentShipperLat = shipperSnap.child("lat").getValue(Double.class);
                Double currentShipperLon = shipperSnap.child("lng").getValue(Double.class);
//                Map<String, Object> shipperInfoMap = shipperSnap.child("shipperInfo").getValue(Map.class);
//                String shipperName= shipperInfoMap.get("shipperName").toString();
//                String shipperPhone = shipperInfoMap.get("shipperPhone").toString();

                String jsCall = String.format(Locale.US,
                        "initOrUpdateMap(%f, %f, %f, %f, '%s')",
                        currentShipperLat, currentShipperLon, // 1, 2: Vị trí shipper (Động)
                        receiverLat, receiverLon,             // 3, 4: Vị trí người nhận (Tĩnh)
                        status);                              // 5: Trạng thái
                webViewMap.evaluateJavascript(jsCall, null);
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
            tvOrderId.setText(orderId);
            tvStatusTag.setText(status);
            tvDriverName.setText(driver);
            tvLicensePlate.setText(license);
            tvDeliveryFeeValue.setText(formatter.format(Double.parseDouble(fee)) + "đ");
            tvDiscountValue.setText(!discount.equals("") ? formatter.format(Double.parseDouble(discount)) + "đ" : "");
            tvTotalValue.setText(formatter.format(Double.parseDouble(total)) + "đ");


            tvOrderIdSummary.setText(orderId);
            tvEstimateArrivalSummary.setText(estimate);
            tvDriverNameSummary.setText(driver);
            tvLicensePlateSummary.setText(license);
            mappingStep(status);
        });
    }
    private void mappingStep(String status) {
        final int ACTIVE_COLOR = Color.parseColor("#EB4D57"); // Màu đỏ active
        if (status.equals(MyConstant.PREPARED)) {
            stepPrepared.setImageResource(R.drawable.ic_prepared_order_active);
            stepPreparedSummary.setImageResource(R.drawable.ic_prepared_order_active);
            tvStatusTag.setText(R.string.prepared);
            tvStatusTagSummary.setText(R.string.prepared);
            tvStatusDescTag.setText(R.string.preparedDesc);
            tvStatusDescTagSummary.setText(R.string.preparedDesc);
        } else if (status.equals(MyConstant.PICKINGUP)) {
            stepPrepared.setImageResource(R.drawable.ic_prepared_order_active);
            stepPreparedSummary.setImageResource(R.drawable.ic_prepared_order_active);
            stepPickingUp.setImageResource(R.drawable.ic_picking_up_order_active);
            stepPickingUpLine.setBackgroundColor(ACTIVE_COLOR);
            stepPickingUpSummary.setImageResource(R.drawable.ic_picking_up_order_active);

            tvStatusTag.setText(R.string.pickingUp);
            tvStatusTagSummary.setText(R.string.pickingUp);
            tvStatusDescTag.setText(R.string.pickingUpDesc);
            tvStatusDescTagSummary.setText(R.string.pickingUpDesc);
            btnCancelOrder.setVisibility(View.GONE);
        } else if (status.equals(MyConstant.DELIVERING)) {
            stepPrepared.setImageResource(R.drawable.ic_prepared_order_active);
            stepPreparedSummary.setImageResource(R.drawable.ic_prepared_order_active);
            stepPickingUp.setImageResource(R.drawable.ic_picking_up_order_active);
            stepPickingUpLine.setBackgroundColor(ACTIVE_COLOR);
            stepPickingUpSummary.setImageResource(R.drawable.ic_picking_up_order_active);
            stepPickingUpSummaryLine.setBackgroundColor(ACTIVE_COLOR);
            stepDelivering.setImageResource(R.drawable.ic_delivering_order_active);
            stepDeliveringSummary.setImageResource(R.drawable.ic_delivering_order_active);
            stepDeliveringLine.setBackgroundColor(ACTIVE_COLOR);
            stepDeliveringSummaryLine.setBackgroundColor(ACTIVE_COLOR);

            tvStatusTag.setText(R.string.delivering);
            tvStatusTagSummary.setText(R.string.delivering);
            tvStatusDescTag.setText(R.string.deliveringDesc);
            tvStatusDescTagSummary.setText(R.string.deliveringDesc);
            btnCancelOrder.setVisibility(View.GONE);
        } else if (status.equals(MyConstant.FINISH)) {
            stepPrepared.setImageResource(R.drawable.ic_prepared_order_active);
            stepPreparedSummary.setImageResource(R.drawable.ic_prepared_order_active);
            stepPickingUp.setImageResource(R.drawable.ic_picking_up_order_active);
            stepPickingUpLine.setBackgroundColor(ACTIVE_COLOR);
            stepPickingUpSummary.setImageResource(R.drawable.ic_picking_up_order_active);
            stepPickingUpSummaryLine.setBackgroundColor(ACTIVE_COLOR);
            stepDelivering.setImageResource(R.drawable.ic_delivering_order_active);
            stepDeliveringLine.setBackgroundColor(ACTIVE_COLOR);
            stepDeliveringSummary.setImageResource(R.drawable.ic_delivering_order_active);
            stepDeliveringSummaryLine.setBackgroundColor(ACTIVE_COLOR);

            stepFinish.setImageResource(R.drawable.ic_finish_order_active);
            stepFinishLine.setBackgroundColor(ACTIVE_COLOR);
            tvStatusTag.setText(R.string.finish);
            tvStatusTagSummary.setText(R.string.finish);
            tvStatusDescTag.setText(R.string.finishDesc);
            tvStatusDescTagSummary.setText(R.string.finishDesc);
            btnCancelOrder.setVisibility(View.GONE);
        }


    }
    private void mappingViewMap() {
        webViewMap = findViewById(R.id.webViewMap);
        webViewMap.getSettings().setJavaScriptEnabled(true);
        webViewMap.getSettings().setAllowFileAccess(true);
        webViewMap.getSettings().setAllowFileAccessFromFileURLs(true);
        webViewMap.getSettings().setAllowUniversalAccessFromFileURLs(true);
// Load map.html trong assets
        webViewMap.loadUrl("file:///android_asset/map.html");

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

    private static void loadPreviewImage(String url, ImageView imageView) {
        if (url == null || url.isEmpty()) return;

        GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                .build());

        Glide.with(imageView.getContext())
                .load(glideUrl)
                .override(600, 600)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(imageView);
    }
}
