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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.OrderProductAdapter;
import vn.haui.android_project.entity.Cart;
import vn.haui.android_project.entity.CartItem;
import vn.haui.android_project.entity.ItemOrderProduct;
import vn.haui.android_project.entity.PaymentCard; // Cần import PaymentCard
import vn.haui.android_project.entity.ProductItem;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.services.DeliveryCalculator;
import vn.haui.android_project.services.FirebaseLocationManager;
import vn.haui.android_project.view.bottomsheet.ChooseVoucherBottomSheet;
import vn.haui.android_project.view.bottomsheet.ChooseVoucherBottomSheet.VoucherSelectionListener;
import vn.haui.android_project.view.bottomsheet.ChoosePaymentBottomSheet;
import vn.haui.android_project.view.bottomsheet.ChoosePaymentBottomSheet.PaymentSelectionListener; // Dùng interface mới

public class ConfirmPaymentActivity extends AppCompatActivity
        implements VoucherSelectionListener, PaymentSelectionListener { // Implement cả hai interface

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
    private TextView tvVoucherCode;
    private TextView tvVoucherDiscount;

    // --- Payment Views ---
    private TextView tvTapToChangePayment;
    private TextView tvPaymentType; // Hiển thị loại thẻ (VISA/Cash)
    private TextView tvPaymentDetails; // Hiển thị số thẻ/chi tiết COD
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
    private String newAddressDetail, newContact, newTitle, phoneNumber, idLocation;
    private double latitude, longitude;

    double pickupLat = 21.0285;
    double pickupLon = 105.8542;
    private String codeVoucher;

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

        mapViews();
        loadData();
        setupListeners();
        registerLocationSelectionLauncher();

        mapDeliveryViews();
        setupDeliveryListeners();
    }

    private void mapViews() {
        // Header
        ImageButton btnBack = findViewById(R.id.btn_back);

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

        // VOUCHER VIEWS
        tvTapToChangeVoucher = findViewById(R.id.tv_tap_to_add_voucher);
        tvVoucherCode = findViewById(R.id.tv_voucher_code);
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
                            idLocation = data.getStringExtra("id_location");
                            firebaseLocationManager.getLocationById(authUser.getUid(), idLocation, (isSuccess, defaultAddress) -> {
                                if (isSuccess) {
                                    if (defaultAddress != null) {
                                        mappingLocation(defaultAddress);
                                    }
                                }
                            });
                        }
                    }
                }
        );
    }

    private void mappingLocation(UserLocationEntity defaultAddress) {
        newAddressDetail = defaultAddress.getAddress();
        newContact = defaultAddress.getPhoneNumber();
        newTitle = defaultAddress.getLocationType();
        phoneNumber = defaultAddress.getRecipientName();
        idLocation = defaultAddress.getId();
        latitude = defaultAddress.getLatitude();
        longitude = defaultAddress.getLongitude();
        if (tvLocationTitle != null) tvLocationTitle.setText(newTitle);
        if (tvAddressDetail != null) tvAddressDetail.setText(newAddressDetail);
        if (tvRecipientContact != null) tvRecipientContact.setText(newContact);
        if (tvRecipientPhone != null) tvRecipientPhone.setText(phoneNumber);
        if ("Home".equals(newTitle)) {
            imgLocationIcon.setImageResource(R.drawable.ic_marker_home);
        } else if ("Work".equals(newTitle)) {
            imgLocationIcon.setImageResource(R.drawable.ic_marker_work);
        } else {
            imgLocationIcon.setImageResource(R.drawable.ic_marker);
        }
        // tinh thoi gian giao hang
        double estimatedTimeMinutes = DeliveryCalculator.calculateEstimatedTime(
                pickupLat,
                pickupLon,
                latitude,
                longitude
        );
        String timeDisplay = DeliveryCalculator.formatTime(estimatedTimeMinutes);
        tvStandardTime.setText(timeDisplay);
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnPlaceOrder.setOnClickListener(v -> placeOrder());

        if (tvTapToChange != null) {
            tvTapToChange.setOnClickListener(v ->
                    {
                        Intent intent = new Intent(this, ChooseRecipientActivity.class); // Giả định Activity này tồn tại
                        locationSelectionLauncher.launch(intent);
                    }
            );
        }

        // BẮT SỰ KIỆN MỞ VOUCHER BOTTOM SHEET
        if (tvTapToChangeVoucher != null) {
            tvTapToChangeVoucher.setOnClickListener(v -> showVoucherBottomSheet());
        }

        // BẮT SỰ KIỆN MỞ PAYMENT BOTTOM SHEET
        if (tvTapToChangePayment != null) {
            tvTapToChangePayment.setOnClickListener(v -> showPaymentBottomSheet());
        }
    }

    // HÀM MỞ PAYMENT BOTTOM SHEET
    private void showPaymentBottomSheet() {
        ChoosePaymentBottomSheet bottomSheet = ChoosePaymentBottomSheet.newInstance(this);
        bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
    }

    private void showVoucherBottomSheet() {
        ChooseVoucherBottomSheet bottomSheet = ChooseVoucherBottomSheet.newInstance(this);
        bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
    }

    @Override
    public void onVoucherSelected(String voucherCode, String discountAmount) {
        if (tvVoucherCode != null) {
            tvVoucherCode.setText(voucherCode);
        }
        if (tvVoucherDiscount != null) {
            tvVoucherDiscount.setText(discountAmount);
        }

        Toast.makeText(this, "Voucher " + voucherCode + " applied! (" + discountAmount + ")", Toast.LENGTH_SHORT).show();
        codeVoucher = voucherCode;
        // Cần gọi hàm tính toán lại tổng tiền thực tế
        updateSummary(productList);
    }

    @Override
    public void onCardSelected(PaymentCard selectedCard) {
        if (selectedCard == null) return;
        if (tvPaymentType != null) {
            tvPaymentType.setText(selectedCard.getCardType());
        }
        if (tvPaymentDetails != null) {
            String fullNumber = selectedCard.getCardNumber();
            String last4Digits = fullNumber != null && fullNumber.length() >= 4
                    ? fullNumber.substring(fullNumber.length() - 4)
                    : "••••";
            tvPaymentDetails.setText("•••• " + last4Digits);
        }
        if (ivPaymentIcon != null) {
            int iconResId = getCardIconResId(selectedCard.getCardType());
            ivPaymentIcon.setImageResource(iconResId);
        }
    }

    @Override
    public void onCashSelected() {
        if (tvPaymentType != null) {
            tvPaymentType.setText(R.string.cash);
        }
        if (tvPaymentDetails != null) {
            tvPaymentDetails.setText(R.string.cash_on_delivery);
        }
        if (ivPaymentIcon != null) {
            ivPaymentIcon.setImageResource(R.drawable.ic_credit_card);
        }
    }


    private int getCardIconResId(String cardType) {
        if (cardType == null) return R.drawable.ic_credit_card;
        if ("VISA".equalsIgnoreCase(cardType)) {
            return R.drawable.ic_visa;
        } else if ("MASTERCARD".equalsIgnoreCase(cardType)) {
            return R.drawable.ic_mastercard;
        } else if ("JCB".equalsIgnoreCase(cardType)) {
            return R.drawable.ic_jcb;
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
                mappingLocation(defaultAddress);
            }
        });
        productList = new ArrayList<>();
        productAdapter = new OrderProductAdapter(productList); // KHỞI TẠO NGAY
        recyclerOrderItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrderItems.setAdapter(productAdapter); // GÁN NGAY
        DatabaseReference cartRef = FirebaseDatabase.getInstance()
                .getReference("carts")
                .child(authUser.getUid());

        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                Cart cart = snapshot.getValue(Cart.class);
                if (cart != null && cart.items != null) {
                    for (CartItem item : cart.items) {
                        ProductItem productItem = item.item_details;
                        if (productItem != null) { // Thêm kiểm tra null để an toàn hơn
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
                updateSummary(productList); // Cập nhật tổng tiền về 0
            }
        });
//        etNoteToRestaurant.setText("No onions in Pizza Margherita, please.");
    }


    private void updateSummary(List<ItemOrderProduct> products) {
        double subTotal = 0;
        double discount = 0;
        for (ItemOrderProduct p : products) {
            subTotal += p.getTotalPrice();
        }
        //tinh ma giam gia
        if ("FIRSTBITE".equals(codeVoucher)) {
            discount = 10000;
        } else if ("WEEKEND20".equals(codeVoucher)) {
            discount = 20000;
        } else if ("LOYALTYL".equals(codeVoucher)) {
            discount = subTotal * 0.15;
        } else if ("FAMILYBON".equals(codeVoucher)) {
            discount = 5000;
        } else if ("NEWMEMBER".equals(codeVoucher)) {
            discount = 50000;
        }
        double deliveryFee = 5.00;
        double finalTotal = subTotal - discount + deliveryFee;

        DecimalFormat formatter = new DecimalFormat("#,###");
        String priceText = formatter.format(finalTotal) + "đ";
        String allItemsText = formatter.format(subTotal) + "đ";
        String deliveryFeeText = formatter.format(deliveryFee) + "đ";
        String discountText = "-" + formatter.format(discount) + "đ";

        tvAllItemsValue.setText(allItemsText);
        tvDeliveryFeeValue.setText(deliveryFeeText);
        tvDiscountValue.setText(discountText);
        tvTotalValue.setText(priceText);
    }

    private void placeOrder() {
        String note = etNoteToRestaurant.getText().toString();
        Toast.makeText(this, "Đã đặt hàng thành công!", Toast.LENGTH_LONG).show();
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
            } else if (v.getId() == R.id.container_schedule_order) {
                containerScheduleOrder.setActivated(true);
                rbScheduleOrder.setChecked(true);
                scheduleTimeSelectionContainer.setVisibility(View.VISIBLE);
                if (ivScheduleDropdown != null)
                    ivScheduleDropdown.setImageResource(R.drawable.ic_arrow_drop_up);
            } else if (v.getId() == R.id.container_pick_up_order) {
                containerPickUpOrder.setActivated(true);
                rbPickUpOrder.setChecked(true);
                pickupTimeSelectionContainer.setVisibility(View.VISIBLE);
                if (ivPickupDropdown != null)
                    ivPickupDropdown.setImageResource(R.drawable.ic_arrow_drop_up);
            }
        };

        if (containerStandardDelivery != null)
            containerStandardDelivery.setOnClickListener(deliveryOptionClickListener);
        if (containerScheduleOrder != null)
            containerScheduleOrder.setOnClickListener(deliveryOptionClickListener);
        if (containerPickUpOrder != null)
            containerPickUpOrder.setOnClickListener(deliveryOptionClickListener);

        if (containerStandardDelivery != null) {
            containerStandardDelivery.callOnClick();
        }

        List<TextView> scheduleChipList = new ArrayList<>();
        if (tvSchedule15min != null) scheduleChipList.add(tvSchedule15min);
        if (tvSchedule30min != null) scheduleChipList.add(tvSchedule30min);
        if (tvSchedule45min != null) scheduleChipList.add(tvSchedule45min);
        if (tvSchedule1hour != null) scheduleChipList.add(tvSchedule1hour);
        TextView[] scheduleChips = scheduleChipList.toArray(new TextView[0]);

        List<TextView> pickupChipList = new ArrayList<>();
        if (tvPickup1000 != null) pickupChipList.add(tvPickup1000);
        if (tvPickup1030 != null) pickupChipList.add(tvPickup1030);
        if (tvPickup1100 != null) pickupChipList.add(tvPickup1100);
        if (tvPickup1130 != null) pickupChipList.add(tvPickup1130);
        TextView[] pickupChips = pickupChipList.toArray(new TextView[0]);

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
            chip.setBackgroundResource(R.drawable.bg_time_chip_default); // Giả định drawable này tồn tại
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
    }

    private void handleTimeChipSelection(TextView selectedChip, TextView... chipGroup) {
        resetTimeChips(chipGroup);
        selectedChip.setBackgroundResource(R.drawable.bg_time_chip_selected); // Giả định drawable này tồn tại
        selectedChip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        Toast.makeText(this, "Selected time: " + selectedChip.getText(), Toast.LENGTH_SHORT).show();
    }
}