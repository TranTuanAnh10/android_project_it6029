package vn.haui.android_project.view.bottomsheet;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import vn.haui.android_project.R;

public class ChooseVoucherBottomSheet extends BottomSheetDialogFragment {

    // --- 1. INTERFACE để gửi dữ liệu về Activity ---
    public interface VoucherSelectionListener {
        void onVoucherSelected(String voucherCode, String discountAmount);
    }
    private VoucherSelectionListener listener;

    // --- 2. Khởi tạo Listener khi BottomSheet được đính kèm (attached) ---
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Đảm bảo Activity/Fragment implement Interface
        if (context instanceof VoucherSelectionListener) {
            listener = (VoucherSelectionListener) context;
        } else if (getParentFragment() instanceof VoucherSelectionListener) {
            listener = (VoucherSelectionListener) getParentFragment();
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement VoucherSelectionListener");
        }
    }

    // --- 3. Phương thức khởi tạo đơn giản ---
    public static ChooseVoucherBottomSheet newInstance(Fragment targetFragment) {
        ChooseVoucherBottomSheet fragment = new ChooseVoucherBottomSheet();
        if (targetFragment != null) {
            fragment.setTargetFragment(targetFragment, 0);
        }
        return fragment;
    }

    // Nếu gọi từ Activity:
    public static ChooseVoucherBottomSheet newInstance(Context context) {
        return new ChooseVoucherBottomSheet();
    }


    // --- 4. Cài đặt Layout ---
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_choose_voucher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Nút Đóng BottomSheet (btn_close là ID bạn nên đặt trong layout)
        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());

        // --- Cài đặt Dữ liệu Voucher (Bước mới) ---
        setupVouchersData(view);

        // --- Xử lý click cho các Item Voucher cố định (Sử dụng các ID đã include) ---

        // Voucher 1: First Bite Free
        View voucherItem1 = view.findViewById(R.id.item_voucher_1);
        if (voucherItem1 != null) {
            voucherItem1.setOnClickListener(v -> handleVoucherSelection("FIRSTBITE", "$10 Off"));
        }

        // Voucher 2: Weekend Feast Discount
        View voucherItem2 = view.findViewById(R.id.item_voucher_2);
        if (voucherItem2 != null) {
            voucherItem2.setOnClickListener(v -> handleVoucherSelection("WEEKEND20", "$20 Off"));
        }

        // Voucher 3: Loyalty Lunch Rewards
        View voucherItem3 = view.findViewById(R.id.item_voucher_3);
        if (voucherItem3 != null) {
            voucherItem3.setOnClickListener(v -> handleVoucherSelection("LOYALTYL", "15% Off"));
        }

        // Voucher 4: Family Meal Bonanza
        View voucherItem4 = view.findViewById(R.id.item_voucher_4);
        if (voucherItem4 != null) {
            voucherItem4.setOnClickListener(v -> handleVoucherSelection("FAMILYBON", "$5 Off"));
        }

        // Xử lý nút Apply (Áp dụng Code thủ công)
        Button btnUseCode = view.findViewById(R.id.btn_use_code);
        EditText etVoucherCode = view.findViewById(R.id.et_voucher_code);

        if (btnUseCode != null && etVoucherCode != null) {
            btnUseCode.setOnClickListener(v -> {
                String code = etVoucherCode.getText().toString().toUpperCase();
                // Logic kiểm tra code thủ công (Ví dụ đơn giản)
                if ("NEWMEMBER".equals(code)) {
                    handleVoucherSelection("NEWMEMBER", "50% Off");
                } else {
                    Toast.makeText(getContext(), "Mã giảm giá không hợp lệ.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Gửi dữ liệu voucher được chọn về Activity/Fragment cha
     */
    private void handleVoucherSelection(String code, String discount) {
        if (listener != null) {
            listener.onVoucherSelected(code, discount);
        }
        dismiss(); // Đóng BottomSheet sau khi chọn
    }

    // --- 5. LOGIC CẬP NHẬT DỮ LIỆU CỦA CÁC ITEM VOUCHER ---
    /**
     * Cập nhật nội dung (Title, Description, Icon) cho từng item voucher
     */
    private void setupVouchersData(View rootView) {
        // Lấy ra các View containers đã được include
        View item1 = rootView.findViewById(R.id.item_voucher_1);
        View item2 = rootView.findViewById(R.id.item_voucher_2);
        View item3 = rootView.findViewById(R.id.item_voucher_3);
        View item4 = rootView.findViewById(R.id.item_voucher_4);

        // Cấu trúc dữ liệu mẫu cho 4 voucher
        updateVoucherItem(item1, "First Bite Free (FIRSTBITE)",
                "Giảm $10 cho đơn hàng đầu tiên của bạn. Áp dụng cho đơn tối thiểu $50.",
                R.drawable.ic_abount_yumyard);

        updateVoucherItem(item2, "Weekend Feast (WEEKEND20)",
                "Giảm 20% tối đa $20 vào các ngày cuối tuần (Thứ 7, CN).",
                R.drawable.ic_abount_yumyard);

        updateVoucherItem(item3, "Loyalty Lunch (LOYALTYL)",
                "Giảm 15% cho đơn hàng ăn trưa. Chỉ áp dụng 11h-14h hàng ngày.",
                R.drawable.ic_abount_yumyard);

        updateVoucherItem(item4, "Family Meal (FAMILYBON)",
                "Giảm $5 cho mọi đơn hàng có combo Family Meal.",
                R.drawable.ic_abount_yumyard);
    }

    /**
     * Hàm helper để ánh xạ và thiết lập dữ liệu cho một item voucher cụ thể
     */
    private void updateVoucherItem(View item, String title, String description, int iconResId) {
        if (item != null) {
            TextView tvTitle = item.findViewById(R.id.tv_voucher_title);
            TextView tvDesc = item.findViewById(R.id.tv_voucher_description);
            ImageView ivIcon = item.findViewById(R.id.iv_payment_icon);

            if (tvTitle != null) tvTitle.setText(title);
            if (tvDesc != null) tvDesc.setText(description);
            // Lưu ý: Đảm bảo bạn có các icon placeholder này trong thư mục drawable
            if (ivIcon != null) ivIcon.setImageResource(iconResId);
        }
    }
}