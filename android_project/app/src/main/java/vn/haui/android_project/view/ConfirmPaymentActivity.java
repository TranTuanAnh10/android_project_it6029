package vn.haui.android_project.view;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.OrderProductAdapter;
import vn.haui.android_project.entity.Cart;
import vn.haui.android_project.entity.CartItem;
import vn.haui.android_project.entity.ItemOrderProduct;
import vn.haui.android_project.entity.PaymentCard;
import vn.haui.android_project.entity.ProductItem;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.entity.VoucherEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.enums.MyConstant;
import vn.haui.android_project.services.DeliveryCalculator;
import vn.haui.android_project.services.FirebaseLocationManager;
import vn.haui.android_project.utils.TimeUtils;
import vn.haui.android_project.view.bottomsheet.ChoosePaymentBottomSheet;
import vn.haui.android_project.view.bottomsheet.ChoosePaymentBottomSheet.PaymentSelectionListener;
import vn.haui.android_project.view.bottomsheet.ChooseVoucherBottomSheet;
import vn.haui.android_project.view.bottomsheet.ChooseVoucherBottomSheet.VoucherSelectionListener;

public class ConfirmPaymentActivity extends AppCompatActivity
        implements VoucherSelectionListener, PaymentSelectionListener {

    // --- Recipient Info Views ---
    private TextView tvTapToChange, tvStandardTime;
    private TextView tvLocationTitle;
    private TextView tvAddressDetail;
    private TextView tvRecipientContact, tvRecipientPhone;

    // --- Order Views ---
    private RecyclerView recyclerOrderItems;
    private EditText etNoteToRestaurant;
    private OrderProductAdapter productAdapter;

    // --- VOUCHER VIEWS ---
    private TextView tvTapToChangeVoucher;
    private TextView tvVoucherCode; // Sửa tên biến này cho đúng với XML
    private TextView tvVoucherDiscount;


    // --- Payment Views ---
    private TextView tvTapToChangePayment;
    private TextView tvPaymentType;
    private TextView tvPaymentDetails;
    private ImageView ivPaymentIcon, imgLocationIcon;

    // --- Summary Views ---
    private TextView tvAllItemsValue;
    private TextView tvDeliveryFeeValue;
    private TextView tvDiscountValue;
    private TextView tvTotalValue;
    private Button btnPlaceOrder;

    private List<ItemOrderProduct> productList;

    // --- Delivery Options Views ---
    private ConstraintLayout containerStandardDelivery, containerScheduleOrder, containerPickUpOrder;
    private LinearLayout scheduleTimeSelectionContainer, pickupTimeSelectionContainer;
    private ImageView ivScheduleDropdown, ivPickupDropdown;
    private CheckBox rbStandardDelivery, rbScheduleOrder, rbPickUpOrder;
    private TextView tvSchedule15min, tvSchedule30min, tvSchedule45min, tvSchedule1hour;
    private TextView tvPickup1000, tvPickup1030, tvPickup1100, tvPickup1130;


    private ActivityResultLauncher<Intent> locationSelectionLauncher;

    private FirebaseLocationManager firebaseLocationManager;
    FirebaseUser authUser;
    private UserLocationEntity addressUser = new UserLocationEntity();
    private String timeDisplay;


    double pickupLat = 21.0285;
    double pickupLon = 105.8542;

    private DatabaseReference orderRef;
    private DatabaseReference cartRef;
    private FirebaseDatabase firebaseDatabase;
    FirebaseUser currentUser;

    // --- State variables ---
    private double subTotal = 0;
    private double discount = 0;
    private double deliveryFee = 15000;
    private double finalTotal = 0;
    private VoucherEntity selectedVoucher;
    private PaymentCard paymentCard;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirm_payment);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_confirm_payment), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseApp.initializeApp(this);
        firebaseDatabase = FirebaseDatabase.getInstance();

        mapViews();
        mapDeliveryViews();
        loadData();
        setupListeners();
        registerLocationSelectionLauncher();
        setupDeliveryListeners();
    }

    private void mapViews() {
        // Header
        findViewById(R.id.btn_back);

        // Recipient Info
        tvTapToChange = findViewById(R.id.tv_tap_to_change);
        tvLocationTitle = findViewById(R.id.tv_location_title);
        tvAddressDetail = findViewById(R.id.tv_address_detail);
        tvRecipientContact = findViewById(R.id.tv_recipient_contact);
        tvRecipientPhone = findViewById(R.id.tv_recipient_phone);
        imgLocationIcon = findViewById(R.id.img_location_icon);

        // Order Items
        recyclerOrderItems = findViewById(R.id.recycler_order_items);
        etNoteToRestaurant = findViewById(R.id.et_note_to_restaurant);

        // ========================================================
        // SỬA LỖI Ở ĐÂY: Ánh xạ đúng các TextView voucher
        // ========================================================
        tvTapToChangeVoucher = findViewById(R.id.tv_tap_to_add_voucher);
        tvVoucherCode = findViewById(R.id.tv_voucher_code); // ID này có trong XML
        tvVoucherDiscount = findViewById(R.id.tv_voucher_discount);

        // PAYMENT INFO
        tvTapToChangePayment = findViewById(R.id.tv_tap_to_change_payment);
        tvPaymentType = findViewById(R.id.tv_card_type);
        tvPaymentDetails = findViewById(R.id.tv_card_number);
        ivPaymentIcon = findViewById(R.id.iv_payment_icon);

        // Summary
        tvAllItemsValue = findViewById(R.id.tv_all_items_value);
        tvDeliveryFeeValue = findViewById(R.id.tv_delivery_fee_value);
        tvDiscountValue = findViewById(R.id.tv_discount_value);
        tvTotalValue = findViewById(R.id.tv_total_value);
        btnPlaceOrder = findViewById(R.id.btn_place_order);
        tvStandardTime = findViewById(R.id.tv_standard_time);
    }

    private void registerLocationSelectionLauncher() {
        locationSelectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String idLocation = data.getStringExtra("id_location");
                            firebaseLocationManager.getLocationById(authUser.getUid(), idLocation, (isSuccess, defaultAddress) -> {
                                if (isSuccess && defaultAddress != null) {
                                    this.addressUser = defaultAddress;
                                    mappingLocation(defaultAddress);
                                }
                            });
                        }
                    }
                }
        );
    }

    private void mappingLocation(UserLocationEntity defaultAddress) {
        if (defaultAddress == null) return;
        tvLocationTitle.setText(defaultAddress.getLocationType());
        tvAddressDetail.setText(defaultAddress.getAddress());
        tvRecipientContact.setText(defaultAddress.getRecipientName());
        tvRecipientPhone.setText(defaultAddress.getPhoneNumber());

        if ("Home".equals(defaultAddress.getLocationType())) {
            imgLocationIcon.setImageResource(R.drawable.ic_marker_home);
        } else if ("Work".equals(defaultAddress.getLocationType())) {
            imgLocationIcon.setImageResource(R.drawable.ic_marker_work);
        } else {
            imgLocationIcon.setImageResource(R.drawable.ic_marker);
        }

        double estimatedTimeMinutes = DeliveryCalculator.calculateEstimatedTime(
                pickupLat,
                pickupLon,
                defaultAddress.getLatitude(),
                defaultAddress.getLongitude()
        );
        timeDisplay = DeliveryCalculator.formatTime(estimatedTimeMinutes);
        tvStandardTime.setText(timeDisplay);
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnPlaceOrder.setOnClickListener(v -> placeOrder());
        tvTapToChange.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChooseRecipientActivity.class);
            locationSelectionLauncher.launch(intent);
        });

        // ========================================================
        // SỬA LỖI Ở ĐÂY: Chỉ đặt Listener cho các View có tồn tại
        // ========================================================
        tvTapToChangeVoucher.setOnClickListener(v -> showVoucherBottomSheet());
        // tvVoucherCode ban đầu ẩn đi, nhưng khi hiện ra cũng nên cho phép click để đổi
        if (tvVoucherCode != null) {
            tvVoucherCode.setOnClickListener(v -> showVoucherBottomSheet());
        }

        // BẮT SỰ KIỆN MỞ PAYMENT BOTTOM SHEET
        if (tvTapToChangePayment != null) {
            tvTapToChangePayment.setOnClickListener(v -> showPaymentBottomSheet());
        }
    }

    private void showPaymentBottomSheet() {
        ChoosePaymentBottomSheet bottomSheet = new ChoosePaymentBottomSheet();
        // Giả sử ChoosePaymentBottomSheet đã được sửa để có hàm này
        // Nếu hàm này báo lỗi, bạn cần sửa file ChoosePaymentBottomSheet.java
        bottomSheet.setPaymentSelectionListener(this);
        bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
    }

    private void showVoucherBottomSheet() {
        ChooseVoucherBottomSheet bottomSheet = ChooseVoucherBottomSheet.newInstance(subTotal);
        bottomSheet.setVoucherSelectionListener(this);
        bottomSheet.show(getSupportFragmentManager(), "VoucherBottomSheetTag");
    }

    @Override
    public void onVoucherSelected(VoucherEntity voucher, double discountAmount) {
        if (voucher == null) {
            // Có thể thêm logic để xóa voucher đã chọn nếu người dùng bấm "Bỏ chọn"
            this.selectedVoucher = null;
            this.discount = 0;

            // Reset lại giao diện về ban đầu
            tvTapToChangeVoucher.setText(R.string.tap_to_add); // "Chạm để thêm voucher"
            tvVoucherCode.setVisibility(View.GONE);
            if (tvVoucherDiscount != null) {
                tvVoucherDiscount.setVisibility(View.GONE);
            }
        } else {
            // Khi người dùng chọn một voucher thành công
            this.selectedVoucher = voucher;
            this.discount = discountAmount;

            // ========================================================
            // SỬA LỖI Ở ĐÂY: Thay đổi Text thay vì ẩn View
            // ========================================================
            // 1. Thay đổi dòng chữ mời gọi
            tvTapToChangeVoucher.setText(R.string.tap_to_change); // Thay bằng "Chạm để thay đổi"

            // 2. Hiển thị tên voucher và số tiền giảm giá
            tvVoucherCode.setText(voucher.getName());
            tvVoucherCode.setVisibility(View.VISIBLE);

            if (tvVoucherDiscount != null) {
                tvVoucherDiscount.setText("- " + new DecimalFormat("#,###").format(discountAmount) + "đ");
                tvVoucherDiscount.setVisibility(View.VISIBLE);
            }
        }

        // Luôn tính toán lại tổng tiền sau mỗi lần thay đổi
        updateSummary(this.productList);
    }


    @Override
    public void onCardSelected(PaymentCard selectedCard) {
        if (selectedCard == null) return;
        this.paymentCard = selectedCard;
        tvPaymentType.setText(selectedCard.getCardType());

        String fullNumber = selectedCard.getCardNumber();
        String last4Digits = fullNumber != null && fullNumber.length() >= 4
                ? fullNumber.substring(fullNumber.length() - 4)
                : "****";
        tvPaymentDetails.setText("**** " + last4Digits);
        ivPaymentIcon.setImageResource(getCardIconResId(selectedCard.getCardType()));
    }

    @Override
    public void onCashSelected() {
        tvPaymentType.setText(R.string.cash);
        tvPaymentDetails.setText(R.string.cash_on_delivery);
        ivPaymentIcon.setImageResource(R.drawable.ic_credit_card);

        PaymentCard cashPayment = new PaymentCard();
        cashPayment.setCardType("Cash");
        cashPayment.setCardNumber("Thanh toán khi nhận hàng");
        this.paymentCard = cashPayment;
    }


    private int getCardIconResId(String cardType) {
        if (cardType == null) return R.drawable.ic_credit_card;
        if ("VISA".equalsIgnoreCase(cardType)) {
            return R.drawable.ic_visa;
        } else if ("MASTERCARD".equalsIgnoreCase(cardType)) {
            return R.drawable.ic_mastercard;
        } else if ("JCB".equalsIgnoreCase(cardType)) {
            return R.drawable.ic_jcb;
        } else if ("Cash".equalsIgnoreCase(cardType)) {
            return R.drawable.ic_credit_card;
        }
        return R.drawable.ic_credit_card;
    }

    private void loadData() {
        authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseLocationManager = new FirebaseLocationManager();
        firebaseLocationManager.getDefaultLocation(authUser.getUid(), (isSuccess, defaultAddress) -> {
            if (isSuccess && defaultAddress != null) {
                this.addressUser = defaultAddress;
                mappingLocation(defaultAddress);
            }
        });

        productList = new ArrayList<>();
        productAdapter = new OrderProductAdapter(productList);
        recyclerOrderItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrderItems.setAdapter(productAdapter);

        cartRef = FirebaseDatabase.getInstance().getReference("carts").child(authUser.getUid());
        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                Cart cart = snapshot.getValue(Cart.class);
                if (cart != null && cart.items != null) {
                    for (CartItem item : cart.items) {
                        ProductItem productItem = item.item_details;
                        if (productItem != null) {
                            int quantity = 0;
                            try {
                                quantity = Integer.parseInt(item.quantityToString());
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Lỗi chuyển đổi số lượng: " + item.quantityToString(), e);
                            }
                            productList.add(new ItemOrderProduct(
                                    productItem.getId(),
                                    productItem.getName(),
                                    productItem.getDescription(),
                                    quantity,
                                    productItem.getPrice(),
                                    productItem.getImage()
                            ));
                        }
                    }
                }
                productAdapter.notifyDataSetChanged();
                updateSummary(productList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Lỗi đọc RTDB: " + error.getMessage(), error.toException());
                productList.clear();
                productAdapter.notifyDataSetChanged();
                updateSummary(productList);
            }
        });
    }

    private void updateSummary(List<ItemOrderProduct> products) {
        subTotal = 0;
        if (products != null) {
            for (ItemOrderProduct p : products) {
                subTotal += p.getTotalPrice();
            }
        }
        finalTotal = subTotal - discount + deliveryFee;

        DecimalFormat formatter = new DecimalFormat("#,###");
        String allItemsText = formatter.format(subTotal) + "đ";
        String deliveryFeeText = formatter.format(deliveryFee) + "đ";
        String discountText = "- " + formatter.format(discount) + "đ";
        String totalText = formatter.format(finalTotal) + "đ";

        tvAllItemsValue.setText(allItemsText);
        tvDeliveryFeeValue.setText(deliveryFeeText);

        if (discount > 0) {
            tvDiscountValue.setText(discountText);
            tvDiscountValue.setVisibility(View.VISIBLE);
        } else {
            tvDiscountValue.setVisibility(View.GONE);
        }

        tvTotalValue.setText(totalText);
    }

    private void placeOrder() {
        if (!validateOrder()) {
            return;
        }
        writeOrderToFirebase();
    }


    private void mapDeliveryViews() {
        containerStandardDelivery = findViewById(R.id.container_standard_delivery);
        containerScheduleOrder = findViewById(R.id.container_schedule_order);
        containerPickUpOrder = findViewById(R.id.container_pick_up_order);

        rbStandardDelivery = findViewById(R.id.rb_standard_delivery);
        rbScheduleOrder = findViewById(R.id.rb_schedule_order);
        rbPickUpOrder = findViewById(R.id.rb_pick_up_order);

        scheduleTimeSelectionContainer = findViewById(R.id.schedule_time_selection_container);
        pickupTimeSelectionContainer = findViewById(R.id.pickup_time_selection_container);

        ivScheduleDropdown = findViewById(R.id.iv_schedule_dropdown);
        ivPickupDropdown = findViewById(R.id.iv_pickup_dropdown);

        tvSchedule15min = findViewById(R.id.tv_schedule_15min);
        tvSchedule30min = findViewById(R.id.tv_schedule_30min);
        tvSchedule45min = findViewById(R.id.tv_schedule_45min);
        tvSchedule1hour = findViewById(R.id.tv_schedule_1hour);

        tvPickup1000 = findViewById(R.id.tv_pickup_1000);
        tvPickup1030 = findViewById(R.id.tv_pickup_1030);
        tvPickup1100 = findViewById(R.id.tv_pickup_1100);
        tvPickup1130 = findViewById(R.id.tv_pickup_1130);
    }

    public static final String DELIVERY_TYPE_STANDARD = "StandardDelivery";
    public static final String DELIVERY_TYPE_SCHEDULE = "ScheduleOrder";
    public static final String DELIVERY_TYPE_PICKUP = "PickUpOrder";
    private String currentDeliveryType = DELIVERY_TYPE_STANDARD;
    private TextView[] scheduleChips;
    private TextView[] pickupChips;

    private void setupDeliveryListeners() {
        View.OnClickListener deliveryOptionClickListener = v -> {
            containerStandardDelivery.setActivated(false);
            containerScheduleOrder.setActivated(false);
            containerPickUpOrder.setActivated(false);

            scheduleTimeSelectionContainer.setVisibility(View.GONE);
            if (ivScheduleDropdown != null)
                ivScheduleDropdown.setImageResource(R.drawable.ic_arrow_drop_down);
            pickupTimeSelectionContainer.setVisibility(View.GONE);
            if (ivPickupDropdown != null)
                ivPickupDropdown.setImageResource(R.drawable.ic_arrow_drop_down);

            rbStandardDelivery.setChecked(false);
            rbScheduleOrder.setChecked(false);
            rbPickUpOrder.setChecked(false);

            if (v.getId() == R.id.container_standard_delivery) {
                containerStandardDelivery.setActivated(true);
                rbStandardDelivery.setChecked(true);
                currentDeliveryType = DELIVERY_TYPE_STANDARD;
            } else if (v.getId() == R.id.container_schedule_order) {
                containerScheduleOrder.setActivated(true);
                rbScheduleOrder.setChecked(true);
                scheduleTimeSelectionContainer.setVisibility(View.VISIBLE);
                currentDeliveryType = DELIVERY_TYPE_SCHEDULE;
                if (ivScheduleDropdown != null)
                    ivScheduleDropdown.setImageResource(R.drawable.ic_arrow_drop_up);
            } else if (v.getId() == R.id.container_pick_up_order) {
                containerPickUpOrder.setActivated(true);
                rbPickUpOrder.setChecked(true);
                pickupTimeSelectionContainer.setVisibility(View.VISIBLE);
                currentDeliveryType = DELIVERY_TYPE_PICKUP;
                if (ivPickupDropdown != null)
                    ivPickupDropdown.setImageResource(R.drawable.ic_arrow_drop_up);
            }
        };

        containerStandardDelivery.setOnClickListener(deliveryOptionClickListener);
        containerScheduleOrder.setOnClickListener(deliveryOptionClickListener);
        containerPickUpOrder.setOnClickListener(deliveryOptionClickListener);

        containerStandardDelivery.callOnClick();

        List<TextView> scheduleChipList = new ArrayList<>();
        if (tvSchedule15min != null) scheduleChipList.add(tvSchedule15min);
        if (tvSchedule30min != null) scheduleChipList.add(tvSchedule30min);
        if (tvSchedule45min != null) scheduleChipList.add(tvSchedule45min);
        if (tvSchedule1hour != null) scheduleChipList.add(tvSchedule1hour);
        scheduleChips = scheduleChipList.toArray(new TextView[0]);

        List<TextView> pickupChipList = new ArrayList<>();
        if (tvPickup1000 != null) pickupChipList.add(tvPickup1000);
        if (tvPickup1030 != null) pickupChipList.add(tvPickup1030);
        if (tvPickup1100 != null) pickupChipList.add(tvPickup1100);
        if (tvPickup1130 != null) pickupChipList.add(tvPickup1130);
        pickupChips = pickupChipList.toArray(new TextView[0]);

        if (scheduleChips.length > 0) {
            for (TextView chip : scheduleChips) {
                chip.setOnClickListener(v -> handleTimeChipSelection((TextView) v, scheduleChips));
            }
            handleTimeChipSelection(scheduleChips[0], scheduleChips);
        }

        if (pickupChips.length > 0) {
            for (TextView chip : pickupChips) {
                chip.setOnClickListener(v -> handleTimeChipSelection((TextView) v, pickupChips));
            }
            handleTimeChipSelection(pickupChips[0], pickupChips);
        }
    }

    private void resetTimeChips(TextView... chips) {
        for (TextView chip : chips) {
            chip.setBackgroundResource(R.drawable.bg_time_chip_default);
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
    }

    private String currentScheduleTime = null;
    private String currentPickupTime = null;

    private void handleTimeChipSelection(TextView selectedChip, TextView... chipGroup) {
        resetTimeChips(chipGroup);
        selectedChip.setBackgroundResource(R.drawable.bg_time_chip_selected);
        selectedChip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        String selectedTime = selectedChip.getText().toString();

        if (chipGroup == scheduleChips) {
            currentScheduleTime = selectedTime;
        } else if (chipGroup == pickupChips) {
            currentPickupTime = selectedTime;
        }
    }

    public Map<String, String> getDeliveryDataForDatabase() {
        Map<String, String> deliveryData = new HashMap<>();
        deliveryData.put("deliveryType", currentDeliveryType);

        if (DELIVERY_TYPE_SCHEDULE.equals(currentDeliveryType)) {
            deliveryData.put("scheduledTime", currentScheduleTime != null ? currentScheduleTime : "");
        } else if (DELIVERY_TYPE_PICKUP.equals(currentDeliveryType)) {
            deliveryData.put("pickupTime", currentPickupTime != null ? currentPickupTime : "");
        }
        return deliveryData;
    }

    private static final String DATE_FORMAT = "yyyyMMddHHmmss";
    private static final Random RANDOM = new Random();

    private void writeOrderToFirebase() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String timestamp = sdf.format(new Date());
        int randomNumber = RANDOM.nextInt(1000);
        String randomSuffix = String.format("%03d", randomNumber);
        String orderId = timestamp + "-" + randomSuffix;

        orderRef = firebaseDatabase.getReference(DatabaseTable.ORDERS.getValue()).child(orderId);
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        orderData.put("uid", currentUser.getUid());
        orderData.put("created_at", TimeUtils.getCreationTime());
        orderData.put("status", MyConstant.PREPARED);
        orderData.put("subTotal", subTotal);
        orderData.put("deliveryFee", deliveryFee);
        orderData.put("discount", discount);
        orderData.put("total", finalTotal);
        orderData.put("note", etNoteToRestaurant.getText().toString());
        orderData.put("productList", productList);
        orderData.put("addressUser", addressUser);
        orderData.put("voucher", selectedVoucher);
        orderData.put("paymentCard", paymentCard);
        orderData.put("timeDisplay", timeDisplay);
        orderData.put("delivery", getDeliveryDataForDatabase());

        Map<String, Object> shipperLocation = new HashMap<>();
        shipperLocation.put("lat", 0);
        shipperLocation.put("lng", 0);

        Map<String, Object> storeLocation = new HashMap<>();
        storeLocation.put("lat", pickupLat);
        storeLocation.put("lng", pickupLon);

        Map<String, Object> receiverLocation = new HashMap<>();
        receiverLocation.put("lat", addressUser.getLatitude());
        receiverLocation.put("lng", addressUser.getLongitude());

        orderData.put("shipper", shipperLocation);
        orderData.put("store", storeLocation);
        orderData.put("receiver", receiverLocation);

        orderRef.setValue(orderData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Order data written successfully");
                    clearCartAndFinish(orderId);
                })
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to write order: " + e.getMessage()));
    }

    private void clearCartAndFinish(String orderId) {
        if (cartRef == null) return;
        cartRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Giỏ hàng đã được xóa thành công.");
            } else {
                Log.e(TAG, "Xóa giỏ hàng thất bại.", task.getException());
            }
            Intent intent = new Intent(ConfirmPaymentActivity.this, OrderPlacedActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            startActivity(intent);
            finish();
        });
    }


    private boolean validateOrder() {
        if (addressUser == null || addressUser.getAddress() == null || addressUser.getAddress().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (paymentCard == null) {
            Toast.makeText(this, "Vui lòng chọn hình thức thanh toán", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
