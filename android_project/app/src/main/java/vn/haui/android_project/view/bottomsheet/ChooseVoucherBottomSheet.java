package vn.haui.android_project.view.bottomsheet;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.VoucherAdapter;
import vn.haui.android_project.entity.VoucherEntity;

public class ChooseVoucherBottomSheet extends BottomSheetDialogFragment {

    // --- 1. INTERFACE để gửi dữ liệu về Activity ---
    public interface VoucherSelectionListener {
        void onVoucherSelected(VoucherEntity voucher, double discountAmount);
    }

    private static final String TAG = "VoucherBottomSheet";
    private static final String ARG_TOTAL_BILL = "totalBill";

    private VoucherSelectionListener listener;
    private double totalBill = 0.0;

    private List<VoucherEntity> voucherList = new ArrayList<>();
    private VoucherAdapter voucherAdapter;
    private RecyclerView recyclerView;
    private EditText etVoucherCode;
    private Button btnUseCode;

    // --- 2. Phương thức khởi tạo an toàn để truyền totalBill ---
    public static ChooseVoucherBottomSheet newInstance(double totalBill) {
        ChooseVoucherBottomSheet fragment = new ChooseVoucherBottomSheet();
        Bundle args = new Bundle();
        args.putDouble(ARG_TOTAL_BILL, totalBill);
        fragment.setArguments(args);
        return fragment;
    }

    // Phương thức để Activity/Fragment đăng ký lắng nghe
    public void setVoucherSelectionListener(VoucherSelectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            totalBill = getArguments().getDouble(ARG_TOTAL_BILL, 0.0);
        }
    }

    // --- 3. Cài đặt Layout ---
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_choose_voucher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ View
        recyclerView = view.findViewById(R.id.rv_vouchers);
        etVoucherCode = view.findViewById(R.id.et_voucher_code);
        btnUseCode = view.findViewById(R.id.btn_use_code);
        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());

        setupRecyclerView();
        setupListeners();
        loadVouchers();
    }

    /**
     * Khởi tạo và thiết lập RecyclerView
     */
    private void setupRecyclerView() {
        // Khởi tạo Adapter với totalBill đã nhận được
        voucherAdapter = new VoucherAdapter(getContext(), voucherList, totalBill, (voucher, discountAmount) -> {
            // Đây là lambda triển khai interface VoucherClickListener từ Adapter
            // Khi người dùng click "Áp dụng" trên một item trong RecyclerView
            Log.d(TAG, "Voucher " + voucher.getCode() + " clicked, discount: " + discountAmount);
            handleVoucherSelection(voucher, discountAmount);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(voucherAdapter);
    }

    /**
     * Thiết lập các sự kiện click
     */
    private void setupListeners() {
        btnUseCode.setOnClickListener(v -> {
            String code = etVoucherCode.getText().toString().trim().toUpperCase();
            if (code.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập mã giảm giá", Toast.LENGTH_SHORT).show();
                return;
            }
            applyManualCode(code);
        });
    }

    /**
     * Tải danh sách voucher từ Firestore và cập nhật Adapter
     */
    private void loadVouchers() {
        FirebaseFirestore.getInstance().collection("vouchers")
                .orderBy("minOrderValue", Query.Direction.ASCENDING) // Sắp xếp theo đơn tối thiểu
                .get() // Dùng get() để tải 1 lần, không cần realtime
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null) return;

                    List<VoucherEntity> newVouchers = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        VoucherEntity voucher = doc.toObject(VoucherEntity.class);
                        if (voucher != null) {
                            voucher.setId(doc.getId()); // Gán ID từ Firestore Document
                            newVouchers.add(voucher);
                        }
                    }
                    // Cập nhật dữ liệu vào Adapter
                    voucherAdapter.updateVouchers(newVouchers);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi tải voucher", e));
    }

    /**
     * Xử lý logic khi người dùng nhập code thủ công
     */
    private void applyManualCode(String code) {
        // Tìm voucher trong danh sách đã tải
        VoucherEntity foundVoucher = null;
        for (VoucherEntity voucher : voucherList) {
            if (code.equals(voucher.getCode())) {
                foundVoucher = voucher;
                break;
            }
        }

        if (foundVoucher == null) {
            Toast.makeText(getContext(), "Mã giảm giá không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra xem voucher có áp dụng được không
        if (isVoucherApplicable(foundVoucher, totalBill)) {
            double discountAmount = calculateDiscount(foundVoucher, totalBill);
            handleVoucherSelection(foundVoucher, discountAmount);
        } else {
            Toast.makeText(getContext(), "Voucher không đủ điều kiện áp dụng.", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Gửi dữ liệu voucher được chọn về Activity/Fragment cha
     */
    private void handleVoucherSelection(VoucherEntity voucher, double discountAmount) {
        if (listener != null) {
            listener.onVoucherSelected(voucher, discountAmount);
        }
        dismiss(); // Đóng BottomSheet sau khi chọn
    }

    // --- Các hàm tính toán logic (sao chép từ Adapter để dùng cho code thủ công) ---

    private boolean isVoucherApplicable(VoucherEntity voucher, double bill) {
        if (voucher == null || !voucher.isActive()) {
            return false;
        }
        return bill >= voucher.getMinOrderValue();
    }

    private double calculateDiscount(VoucherEntity voucher, double bill) {
        if (!isVoucherApplicable(voucher, bill)) {
            return 0.0;
        }
        double discount;
        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            discount = bill * (voucher.getDiscountValue() / 100.0);
            if (voucher.getMaxOrderValue() > 0 && discount > voucher.getMaxOrderValue()) {
                discount = voucher.getMaxOrderValue();
            }
        } else {
            discount = voucher.getDiscountValue();
        }
        return discount;
    }
}
