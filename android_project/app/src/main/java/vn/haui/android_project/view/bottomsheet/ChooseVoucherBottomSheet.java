package vn.haui.android_project.view.bottomsheet;

import android.content.Context;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

// Import Adapter và Entity mới (Giả định nằm trong các package này)
import vn.haui.android_project.R;
import vn.haui.android_project.adapter.VoucherAdapter;
import vn.haui.android_project.entity.VoucherEntity;

// Class triển khai VoucherClickListener của Adapter
public class ChooseVoucherBottomSheet extends BottomSheetDialogFragment implements VoucherAdapter.VoucherClickListener {

    // --- 1. INTERFACE để gửi dữ liệu về Activity ---
    public interface VoucherSelectionListener {
        // Tham số thứ hai (discountAmount) trong VoucherEntity hiện tại không có,
        // nên tôi giữ nguyên String, nhưng bạn nên cân nhắc sửa VoucherEntity
        void onVoucherSelected(String voucherCode, Double discountAmount);
    }

    private VoucherSelectionListener listener;
    private FirebaseFirestore db;
    private CollectionReference vouchersRef;

    // Danh sách sẽ được Adapter sử dụng
    private List<VoucherEntity> voucherList;
    private RecyclerView recyclerView;
    private VoucherAdapter voucherAdapter;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Đảm bảo Activity/Fragment implement Interface
        if (context instanceof VoucherSelectionListener) {
            listener = (VoucherSelectionListener) context;
        } else if (getParentFragment() instanceof VoucherSelectionListener) {
            listener = (VoucherSelectionListener) getParentFragment();
        } else {
            // Đây là đoạn code quan trọng để tránh crash nếu Activity/Fragment quên implement
            Log.e("ChooseVoucherBottomSheet", getParentFragment() != null ? getParentFragment().toString() : context.toString() + " must implement VoucherSelectionListener");
            // Không throw RuntimeException để tránh crash cứng BottomSheet, chỉ log lỗi.
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
        db = FirebaseFirestore.getInstance();
        vouchersRef = db.collection("vouchers");

        // Khởi tạo danh sách và Adapter
        voucherList = new ArrayList<>();
        setupRecyclerView(view);

        // Tải dữ liệu từ Firestore
        loadVouchers();

        // Nút Đóng BottomSheet (Giả định ID R.id.btn_close tồn tại)
        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());

        // --- Bỏ qua logic Xử lý click cho các Item Voucher cố định (item_voucher_1 đến item_voucher_4)
        // Vì giờ chúng ta dùng RecyclerView động.

        // Xử lý nút Apply (Áp dụng Code thủ công)
        Button btnUseCode = view.findViewById(R.id.btn_use_code);
        EditText etVoucherCode = view.findViewById(R.id.et_voucher_code);

        if (btnUseCode != null && etVoucherCode != null) {
            btnUseCode.setOnClickListener(v -> {
                String code = etVoucherCode.getText().toString().toUpperCase();

                // Logic kiểm tra code thủ công: Tìm trong danh sách voucher đã tải
                VoucherEntity selectedVoucher = findVoucherByCode(code);

                if (selectedVoucher != null && selectedVoucher.isActive()) {
                    // Giả định title của voucher là discountAmount (ví dụ: "Giảm 50K")
                    handleVoucherSelection(selectedVoucher.getCode(), selectedVoucher.getDiscountValue());
                } else if (selectedVoucher != null && !selectedVoucher.isValid()) {
                    Toast.makeText(getContext(), "Voucher " + code + " không áp dụng được cho đơn hàng này.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Mã giảm giá không hợp lệ.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Khởi tạo và thiết lập RecyclerView
     */
    private void setupRecyclerView(View rootView) {
        // Giả định layout bottom_sheet_choose_voucher có RecyclerView với ID là rv_vouchers
        recyclerView = rootView.findViewById(R.id.rv_vouchers);
        voucherAdapter = new VoucherAdapter(getContext(), voucherList, this); // 'this' là VoucherClickListener
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(voucherAdapter);
        } else {
            Log.e("ChooseVoucherBottomSheet", "Lỗi: Không tìm thấy RecyclerView với ID R.id.rv_vouchers trong layout!");
        }
    }


    /**
     * Gửi dữ liệu voucher được chọn về Activity/Fragment cha
     */
    private void handleVoucherSelection(String code, Double discount) {
        if (listener != null) {
            listener.onVoucherSelected(code, discount);
        }
        dismiss(); // Đóng BottomSheet sau khi chọn
    }

    /**
     * Tải danh sách voucher từ Firestore và cập nhật Adapter
     */
    private void loadVouchers() {
        // Lắng nghe sự thay đổi dữ liệu theo thời gian thực (addSnapshotListener)
        vouchersRef.orderBy("name", Query.Direction.ASCENDING) // Đổi thành title để sắp xếp
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Lỗi tải voucher", error);
                        return;
                    }
                    if (value != null) {
                        List<VoucherEntity> newVouchers = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            VoucherEntity voucher = doc.toObject(VoucherEntity.class);
                            if (voucher != null) {
                                // Gán ID từ Firestore Document cho Entity
                                voucher.setId(doc.getId());

                                // Gán giá trị mặc định cho isApplicable nếu không có trong doc (hoặc tự tính toán logic ở đây)
                                // Giả định tất cả voucher tải về là áp dụng được trừ khi có trường khác quy định
                                // Đây là nơi bạn sẽ thêm logic kiểm tra điều kiện áp dụng thực tế
                                if (voucher.isActive() || voucher.getName().isEmpty()) { // Ví dụ: Giữ nguyên trạng thái từ DB
                                    // Do VoucherEntity không có expiryDate, ta cần đảm bảo trường isApplicable được thiết lập đúng trong DB.
                                }

                                newVouchers.add(voucher);
                            }
                        }
                        // Cập nhật dữ liệu vào Adapter
                        voucherList.clear();
                        voucherList.addAll(newVouchers);
                        voucherAdapter.notifyDataSetChanged();
                    }
                });
    }

    /**
     * Tìm kiếm Voucher theo mã code trong danh sách đã tải.
     */
    private VoucherEntity findVoucherByCode(String code) {
        for (VoucherEntity voucher : voucherList) {
            if (code.equals(voucher.getCode())) {
                return voucher;
            }
        }
        return null;
    }

    // --- 6. TRIỂN KHAI INTERFACE TỪ VOUCHERADAPTER ---

    /**
     * Xử lý khi người dùng nhấn vào nút "Áp dụng" trên một item voucher trong RecyclerView.
     */
    @Override
    public void onVoucherClicked(VoucherEntity voucher) {
        handleVoucherSelection(voucher.getCode(), voucher.getDiscountValue());
    }
}