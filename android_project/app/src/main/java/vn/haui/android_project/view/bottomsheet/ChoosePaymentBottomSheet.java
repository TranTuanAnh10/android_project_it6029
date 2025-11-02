package vn.haui.android_project.view.bottomsheet;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import vn.haui.android_project.R;

public class ChoosePaymentBottomSheet extends BottomSheetDialogFragment {

    public interface PaymentSelectionListener {
        void onPaymentSelected(String paymentType, String details);
    }

    private PaymentSelectionListener listener;
    private ConstraintLayout containerCreditCard, containerCash;
    private TextView tvCreditCardDetails, tvCashDetails;

    public static ChoosePaymentBottomSheet newInstance(PaymentSelectionListener listener) {
        ChoosePaymentBottomSheet fragment = new ChoosePaymentBottomSheet();
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Trong onCreateView, chỉ làm duy nhất một việc là inflate layout
        return inflater.inflate(R.layout.bottom_sheet_choose_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ========================================================
        // SỬA LỖI Ở ĐÂY: Chuyển toàn bộ logic vào onViewCreated
        // ========================================================

        // 1. Ánh xạ các View sau khi layout đã được tạo
        containerCreditCard = view.findViewById(R.id.container_credit_card);
        containerCash = view.findViewById(R.id.container_cash);
        tvCreditCardDetails = view.findViewById(R.id.tv_card_details); // ID của "VISA *3282"
        tvCashDetails = view.findViewById(R.id.tv_cash_details);     // ID của "Cash on delivery"

        // 2. Thiết lập sự kiện click
        if (containerCreditCard != null) {
            containerCreditCard.setOnClickListener(v -> {
                if (listener != null) {
                    // Lấy text từ TextView để gửi về
                    String details = tvCreditCardDetails.getText().toString();
                    listener.onPaymentSelected("Credit Card", details);
                }
                dismiss(); // Đóng BottomSheet sau khi chọn
            });
        }

        if (containerCash != null) {
            containerCash.setOnClickListener(v -> {
                if (listener != null) {
                    // Lấy text từ TextView để gửi về
                    String details = tvCashDetails.getText().toString();
                    listener.onPaymentSelected("Cash on delivery", details);
                }
                dismiss(); // Đóng BottomSheet sau khi chọn
            });
        }
    }

    // Gỡ bỏ đoạn code cũ trong onAttach để tránh lỗi ClassCastException
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Không cần kiểm tra listener ở đây nữa vì ta đã truyền qua newInstance
        // Điều này giúp code linh hoạt và an toàn hơn.
    }
}
