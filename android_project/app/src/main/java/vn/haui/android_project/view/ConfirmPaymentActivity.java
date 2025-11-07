package vn.haui.android_project.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.OrderProductAdapter;
import vn.haui.android_project.entity.OrderProduct;
import vn.haui.android_project.entity.PaymentCard; // Cần import PaymentCard
import vn.haui.android_project.view.bottomsheet.ChooseVoucherBottomSheet;
import vn.haui.android_project.view.bottomsheet.ChooseVoucherBottomSheet.VoucherSelectionListener;
import vn.haui.android_project.view.bottomsheet.ChoosePaymentBottomSheet;
import vn.haui.android_project.view.bottomsheet.ChoosePaymentBottomSheet.PaymentSelectionListener; // Dùng interface mới

public class ConfirmPaymentActivity extends AppCompatActivity
        implements VoucherSelectionListener, PaymentSelectionListener { // Implement cả hai interface

    // --- Recipient Info Views ---
    private TextView tvTapToChange;
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
    private TextView tvAllItems;
    private TextView tvDeliveryFee;
    private TextView tvDiscount;
    private TextView tvTotal;
    private Button btnPlaceOrder;

    private List<OrderProduct> productList;

    // --- Delivery Options Views ---
    private ConstraintLayout containerStandardDelivery, containerScheduleOrder, containerPickUpOrder;
    private LinearLayout scheduleTimeSelectionContainer, pickupTimeSelectionContainer;
    private ImageView ivScheduleDropdown, ivPickupDropdown;
    private RadioButton rbStandardDelivery, rbScheduleOrder, rbPickUpOrder;
    private TextView tvSchedule15min, tvSchedule30min, tvSchedule45min, tvSchedule1hour;
    private TextView tvPickup1000, tvPickup1030, tvPickup1100, tvPickup1130;


    private ActivityResultLauncher<Intent> locationSelectionLauncher;

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
        loadMockData();
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
        ivPaymentIcon = findViewById(R.id.iv_card_icon);

        // Summary
        tvAllItems = findViewById(R.id.tv_all_items);
        tvDeliveryFee = findViewById(R.id.tv_delivery_fee);
        tvDiscount = findViewById(R.id.tv_discount);
        tvTotal = findViewById(R.id.tv_total);
        btnPlaceOrder = findViewById(R.id.btn_place_order);
    }

    private void registerLocationSelectionLauncher() {
        locationSelectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String newAddressDetail = data.getStringExtra("new_address_detail");
                            String newContact = data.getStringExtra("new_recipient_contact");
                            String newTitle = data.getStringExtra("new_location_title");
                            String phoneNumber = data.getStringExtra("new_phone_number");

                            if (tvLocationTitle != null) tvLocationTitle.setText(newTitle);
                            if (tvAddressDetail != null) tvAddressDetail.setText(newAddressDetail);
                            if (tvRecipientContact != null) tvRecipientContact.setText(newContact);
                            if (tvRecipientPhone != null) tvRecipientPhone.setText(phoneNumber);
                            if ("Home".equals(newTitle)) {
                                imgLocationIcon.setImageResource(R.drawable.ic_marker_home); // Giả định icon này tồn tại
                            } else if ("Work".equals(newTitle)) {
                                imgLocationIcon.setImageResource(R.drawable.ic_marker_work); // Giả định icon này tồn tại
                            } else {
                                imgLocationIcon.setImageResource(R.drawable.ic_marker); // Giả định icon này tồn tại
                            }
                            Toast.makeText(this, "Địa chỉ mới đã được cập nhật!", Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
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
        // 'this' là ConfirmPaymentActivity, đã implement PaymentSelectionListener
        ChoosePaymentBottomSheet bottomSheet = ChoosePaymentBottomSheet.newInstance(this);
        bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
    }

    // HÀM MỞ VOUCHER BOTTOM SHEET
    private void showVoucherBottomSheet() {
        ChooseVoucherBottomSheet bottomSheet = ChooseVoucherBottomSheet.newInstance(this);
        bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
    }

    // ==========================================================
    // TRIỂN KHAI INTERFACE VOUCHERSELECTIONLISTENER
    // ==========================================================
    @Override
    public void onVoucherSelected(String voucherCode, String discountAmount) {
        if (tvVoucherCode != null) {
            tvVoucherCode.setText("Code " + voucherCode);
        }
        if (tvVoucherDiscount != null) {
            tvVoucherDiscount.setText(discountAmount);
        }

        Toast.makeText(this, "Voucher " + voucherCode + " applied! (" + discountAmount + ")", Toast.LENGTH_SHORT).show();

        // Cần gọi hàm tính toán lại tổng tiền thực tế
        // updateSummary(productList);
    }

    // ==========================================================
    // TRIỂN KHAI INTERFACE PAYMENTSELECTIONLISTENER (MỚI)
    // ==========================================================

    // 🏆 HÀM MỚI KHI CHỌN CREDIT CARD/THẺ GHI NỢ
    @Override
    public void onCardSelected(PaymentCard selectedCard) {
        if (selectedCard == null) return;

        // 1. Cập nhật Loại thẻ (VISA, MASTERCARD,...)
        if (tvPaymentType != null) {
            tvPaymentType.setText(selectedCard.getCardType());
        }

        // 2. Cập nhật 4 số cuối (Details)
        if (tvPaymentDetails != null) {
            String fullNumber = selectedCard.getCardNumber();
            String last4Digits = fullNumber != null && fullNumber.length() >= 4
                    ? fullNumber.substring(fullNumber.length() - 4)
                    : "••••";
            // Hiển thị format "**** 1234"
            tvPaymentDetails.setText("•••• " + last4Digits);
        }

        // 3. Cập nhật Icon (dựa trên loại thẻ)
        if (ivPaymentIcon != null) {
            int iconResId = getCardIconResId(selectedCard.getCardType());
            ivPaymentIcon.setImageResource(iconResId);
        }

        Toast.makeText(this, "Đã chọn thẻ " + selectedCard.getCardType() + " •••• " + selectedCard.getLast4Digits(), Toast.LENGTH_SHORT).show();
    }

    // 🏆 HÀM MỚI KHI CHỌN CASH ON DELIVERY
    @Override
    public void onCashSelected() {
        // 1. Cập nhật Type
        if (tvPaymentType != null) {
            tvPaymentType.setText("Cash");
        }
        // 2. Cập nhật Details
        if (tvPaymentDetails != null) {
            tvPaymentDetails.setText("Cash on delivery");
        }
        // 3. Cập nhật Icon (ic_prepared_order_active là icon tạm thời cho COD)
        if (ivPaymentIcon != null) {
            ivPaymentIcon.setImageResource(R.drawable.ic_credit_card);
        }
      }

    /**
     * Hàm hỗ trợ ánh xạ loại thẻ (String) sang Resource ID (Icon)
     */
    private int getCardIconResId(String cardType) {
        if (cardType == null) return R.drawable.ic_credit_card; // Giả định icon mặc định
        // Sử dụng ignoreCase để đảm bảo khớp
        if ("VISA".equalsIgnoreCase(cardType)) {
            return R.drawable.ic_visa; // Giả định icon này tồn tại
        } else if ("MASTERCARD".equalsIgnoreCase(cardType)) {
            return R.drawable.ic_mastercard; // Giả định icon này tồn tại
        } else if ("JCB".equalsIgnoreCase(cardType)) {
            return R.drawable.ic_jcb; // Giả định icon này tồn tại
        }
        return R.drawable.ic_credit_card; // Icon mặc định
    }

    // ==========================================================
    // CÁC HÀM CƠ BẢN KHÁC (GIỮ NGUYÊN)
    // ==========================================================

    private void loadMockData() {
        tvLocationTitle.setText("Your Location (Office)");
        tvAddressDetail.setText("3891 Le Thanh Nghi, Hai Ba Trung, Ha Noi...");
        tvRecipientContact.setText("Nguyen Van A - 0987654321");

        productList = new ArrayList<>();
        productList.add(new OrderProduct("Pizza Margherita", "Large size, extra cheese", 2, 35.0));
        productList.add(new OrderProduct("Pizza Pepperoni", "Medium size, extra sauce", 1, 30.0));
        productList.add(new OrderProduct("Sprite", "Can", 3, 5.0));

        productAdapter = new OrderProductAdapter(productList);
        recyclerOrderItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrderItems.setAdapter(productAdapter);

        etNoteToRestaurant.setText("No onions in Pizza Margherita, please.");

        updateSummary(productList);
    }

    private void updateSummary(List<OrderProduct> products) {
        double subTotal = 0;
        for (OrderProduct p : products) {
            subTotal += p.getTotalPrice();
        }

        double discount = 15.00;
        double deliveryFee = 5.00;
        double finalTotal = subTotal - discount + deliveryFee;

        tvAllItems.setText("All items $" + String.format("%.2f", subTotal));
        tvDeliveryFee.setText("Delivery fee $" + String.format("%.2f", deliveryFee));
        tvDiscount.setText("Discount -$" + String.format("%.2f", discount));
        tvTotal.setText("Total $" + String.format("%.2f", finalTotal));
    }

    private void placeOrder() {
        String note = etNoteToRestaurant.getText().toString();
        Toast.makeText(this, "Đã đặt hàng thành công! Ghi chú: " + note, Toast.LENGTH_LONG).show();
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