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

// 1. IMPORT CÁC BOTTOM SHEET VÀ INTERFACE
import vn.haui.android_project.view.bottomsheet.ChooseVoucherBottomSheet;
import vn.haui.android_project.view.bottomsheet.ChooseVoucherBottomSheet.VoucherSelectionListener;
import vn.haui.android_project.view.bottomsheet.ChooseVoucherBottomSheet.VoucherSelectionListener;
import vn.haui.android_project.view.bottomsheet.ChoosePaymentBottomSheet;
import vn.haui.android_project.view.bottomsheet.ChoosePaymentBottomSheet.PaymentSelectionListener;

// 2. IMPLEMENT INTERFACE VOUCHERSELECTIONLISTENER
public class ConfirmPaymentActivity extends AppCompatActivity
        implements VoucherSelectionListener, PaymentSelectionListener {

    // --- Recipient Info Views ---
    private TextView tvTapToChange;
    private TextView tvLocationTitle;
    private TextView tvAddressDetail;
    private TextView tvRecipientContact, tvRecipientPhone;

    // --- Order Views ---
    private RecyclerView recyclerOrderItems;
    private EditText etNoteToRestaurant;
    private OrderProductAdapter productAdapter;

    // --- VOUCHER VIEWS MỚI ---
    private TextView tvTapToChangeVoucher;
    private TextView tvVoucherCode;
    private TextView tvVoucherDiscount;

    // --- Payment Views
    private TextView tvTapToChangePayment;
    private TextView tvPaymentType;
    private TextView tvPaymentDetails;
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
        registerLocationSelectionLauncher(); // Đăng ký ActivityResultLauncher(chon dia chi)

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

        // --- ÁNH XẠ VOUCHER VIEWS ---
        tvTapToChangeVoucher = findViewById(R.id.tv_tap_to_add_voucher); // ID Nút "Tap to add"
        tvVoucherCode = findViewById(R.id.tv_voucher_code);           // ID Code: EG...
        tvVoucherDiscount = findViewById(R.id.tv_voucher_discount);   // ID Discount: $15 Off
// --- PAYMENT INFO (TỪ activity_confirm_payment.xml) ---
        tvTapToChangePayment = findViewById(R.id.tv_tap_to_change_payment); // ID Nút "Tap to change"
        tvPaymentType = findViewById(R.id.tv_card_type);         // ID Type: Credit Card
        tvPaymentDetails = findViewById(R.id.tv_card_number);    // ID Details: *3282
        ivPaymentIcon = findViewById(R.id.iv_card_icon);      // ID Icon: VISA/COD
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
                    // Kiểm tra xem có kết quả thành công được trả về không
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            // Lấy dữ liệu địa chỉ mới từ Intent
                            String newAddressDetail = data.getStringExtra("new_address_detail");
                            String newContact = data.getStringExtra("new_recipient_contact");
                            String newTitle = data.getStringExtra("new_location_title");
                            String phoneNumber = data.getStringExtra("new_phone_number");

                            // Cập nhật giao diện trong ConfirmPaymentActivity
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
                            Toast.makeText(this, "Địa chỉ mới đã được cập nhật!", Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnPlaceOrder.setOnClickListener(v -> placeOrder());

        // Bắt sự kiện click vào khu vực thay đổi người nhận
        if (tvTapToChange != null) {
            tvTapToChange.setOnClickListener(v ->
                    {
                        Intent intent = new Intent(this, ChooseRecipientActivity.class);
                        locationSelectionLauncher.launch(intent);
                    }
            );
        }

        // --- BẮT SỰ KIỆN MỞ VOUCHER BOTTOM SHEET ---
        if (tvTapToChangeVoucher != null) {
            tvTapToChangeVoucher.setOnClickListener(v -> showVoucherBottomSheet());
        }

        // --- BẮT SỰ KIỆN MỞ PAYMENT BOTTOM SHEET ---
        if (tvTapToChangePayment != null) {
            tvTapToChangePayment.setOnClickListener(v -> showPaymentBottomSheet());
        }
    }

    // HÀM MỞ PAYMENT BOTTOM SHEET
    private void showPaymentBottomSheet() {
        ChoosePaymentBottomSheet bottomSheet = ChoosePaymentBottomSheet.newInstance(this);
        bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
    }

    // HÀM MỞ VOUCHER BOTTOM SHEET
    private void showVoucherBottomSheet() {
        // Dùng newInstance(this) vì Activity này đã implement VoucherSelectionListener
        ChooseVoucherBottomSheet bottomSheet = ChooseVoucherBottomSheet.newInstance(this);
        bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
    }

    // 3. TRIỂN KHAI HÀM CỦA INTERFACE VOUCHERSELECTIONLISTENER
    @Override
    public void onVoucherSelected(String voucherCode, String discountAmount) {
        // Cập nhật UI trong ConfirmPaymentActivity
        if (tvVoucherCode != null) {
            tvVoucherCode.setText("Code " + voucherCode);
        }
        if (tvVoucherDiscount != null) {
            tvVoucherDiscount.setText(discountAmount);
        }

        Toast.makeText(this, "Voucher " + voucherCode + " applied! (" + discountAmount + ")", Toast.LENGTH_SHORT).show();

        // Thường thì bạn sẽ gọi một hàm để tính toán lại tổng tiền ở đây
        // Ví dụ: updateSummary(productList, discountAmount);
    }

    @Override
    public void onPaymentSelected(String paymentType, String details) {
        // 1. Cập nhật Type (Credit Card / Cash on delivery)
        if (tvPaymentType != null) {
            // Cập nhật tvPaymentType để hiển thị tên thẻ (VISA/MASTERCARD) hoặc Cash on delivery
            if (paymentType.equals("Credit Card")) {
                // Lấy tên thẻ (VD: "VISA")
                String[] parts = details.split(" ");
                tvPaymentType.setText(parts[0]);
            } else {
                tvPaymentType.setText(paymentType); // Hiển thị "Cash on delivery"
            }
        }

        // 2. Cập nhật Details (số thẻ hoặc chi tiết) và Icon
        if (tvPaymentDetails != null) {
            if (paymentType.equals("Credit Card")) {
                // Lấy 4 số cuối (VD: "*3282")
                String[] parts = details.split(" ");
                String lastDigits = parts[parts.length - 1];

                // Layout gốc của bạn có Text là "VISA •3282", nên giữ format này
                tvPaymentDetails.setText("•" + lastDigits.replace("*", "")); // Hiển thị •3282

                // PHẢI CÓ FILE NÀY!
                ivPaymentIcon.setImageResource(R.drawable.ic_abount_yumyard);

            } else { // Cash on delivery
                tvPaymentDetails.setText(details); // Hiển thị "Cash on delivery"

                // PHẢI CÓ FILE NÀY!
                ivPaymentIcon.setImageResource(R.drawable.ic_prepared_order_active);
            }
        }
        Toast.makeText(this, "Payment method updated: " + details, Toast.LENGTH_SHORT).show();
    }

    private void loadMockData() {
        // --- 1. Dữ liệu Người nhận ---
        tvLocationTitle.setText("Your Location (Office)");
        tvAddressDetail.setText("3891 Le Thanh Nghi, Hai Ba Trung, Ha Noi...");
        tvRecipientContact.setText("Nguyen Van A - 0987654321");

        // --- 2. Dữ liệu Danh sách Sản phẩm ---
        productList = new ArrayList<>();
        productList.add(new OrderProduct("Pizza Margherita", "Large size, extra cheese", 2, 35.0));
        productList.add(new OrderProduct("Pizza Pepperoni", "Medium size, extra sauce", 1, 30.0));
        productList.add(new OrderProduct("Sprite", "Can", 3, 5.0));

        productAdapter = new OrderProductAdapter(productList);
        recyclerOrderItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrderItems.setAdapter(productAdapter);

        etNoteToRestaurant.setText("No onions in Pizza Margherita, please.");

        // --- 3. Dữ liệu Tóm tắt (Summary) ---
        updateSummary(productList);
    }

    private void updateSummary(List<OrderProduct> products) {
        double subTotal = 0;
        for (OrderProduct p : products) {
            subTotal += p.getTotalPrice();
        }

        double discount = 15.00;
        double deliveryFee = 5.00; // Giả định phí giao hàng là $5
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
        // ... (Giữ nguyên logic Delivery) ...
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
            chip.setBackgroundResource(R.drawable.bg_time_chip_default);
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
    }

    private void handleTimeChipSelection(TextView selectedChip, TextView... chipGroup) {
        resetTimeChips(chipGroup);
        selectedChip.setBackgroundResource(R.drawable.bg_time_chip_selected);
        selectedChip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        Toast.makeText(this, "Selected time: " + selectedChip.getText(), Toast.LENGTH_SHORT).show();
    }

    // XÓA CÁC HÀM XỬ LÝ VOUCHER DƯ THỪA TỪ BOTTOM SHEET RA KHỎI ACTIVITY
    /*
    private void setupVouchersData(View rootView) { ... }
    private void updateVoucherItem(View item, String title, String description, int iconResId) { ... }
    */
}