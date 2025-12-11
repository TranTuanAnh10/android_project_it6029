package vn.haui.android_project.view;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.VoucherManagementAdapter;
import vn.haui.android_project.entity.VoucherEntity;

public class VoucherManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private VoucherManagementAdapter adapter;
    private List<VoucherEntity> voucherList;
    private List<VoucherEntity> voucherListOriginal; // Danh sách gốc để search

    private FirebaseFirestore db;
    private CollectionReference vouchersRef;

    private long selectedExpiryTimestamp = 0;

    // Sử dụng SearchView thay vì EditText
    private SearchView searchView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_voucher_management, container, false);

        // 1. Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();
        vouchersRef = db.collection("vouchers");

        // 2. Thiết lập RecyclerView
        recyclerView = view.findViewById(R.id.recycler_vouchers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        voucherList = new ArrayList<>();
        voucherListOriginal = new ArrayList<>();

        adapter = new VoucherManagementAdapter(getContext(), voucherList, new VoucherManagementAdapter.OnVoucherActionClickListener() {
            @Override
            public void onEditClick(VoucherEntity voucher) {
                showVoucherDialog(voucher);
            }

            @Override
            public void onDeleteClick(VoucherEntity voucher) {
                // Confirm delete từ nút xóa bên ngoài list (nếu có)
                confirmDelete(voucher, null);
            }
        });
        recyclerView.setAdapter(adapter);

        // 3. Xử lý nút Back và Tìm kiếm
        try {
            ImageButton btnBack = view.findViewById(R.id.btn_back_voucher);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                });
            }

            // Ánh xạ SearchView
            searchView = view.findViewById(R.id.search_voucher);
            if (searchView != null) {
                // Cấu hình SearchView
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        // Gọi khi bấm nút search trên bàn phím
                        filterVouchers(query);
                        searchView.clearFocus(); // Ẩn bàn phím
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        // Gọi liên tục khi gõ phím
                        filterVouchers(newText);
                        return true;
                    }
                });
            }
        } catch (Exception e) {
            Log.e("VoucherFragment", "Lỗi ánh xạ toolbar: " + e.getMessage());
        }

        // 4. Nút thêm mới
        FloatingActionButton fab = view.findViewById(R.id.fab_add_voucher);
        fab.setOnClickListener(v -> showVoucherDialog(null));

        // 5. Load dữ liệu
        loadVouchers();

        return view;
    }

    // --- Hàm lọc tìm kiếm ---
    private void filterVouchers(String text) {
        if (voucherListOriginal.isEmpty()) return;

        voucherList.clear();
        if (TextUtils.isEmpty(text)) {
            voucherList.addAll(voucherListOriginal);
        } else {
            String query = text.toLowerCase().trim();
            for (VoucherEntity item : voucherListOriginal) {
                if ((item.getCode() != null && item.getCode().toLowerCase().contains(query)) ||
                        (item.getName() != null && item.getName().toLowerCase().contains(query))) {
                    voucherList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // --- Load Data từ Firestore ---
    private void loadVouchers() {
        vouchersRef.orderBy("expiryDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Lỗi tải voucher", error);
                        return;
                    }
                    if (value != null) {
                        voucherListOriginal.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            VoucherEntity voucher = doc.toObject(VoucherEntity.class);
                            if (voucher != null) {
                                voucher.setId(doc.getId());
                                voucherListOriginal.add(voucher);
                            }
                        }

                        // Refresh lại list hiển thị dựa trên từ khóa tìm kiếm hiện tại
                        if (searchView != null && !TextUtils.isEmpty(searchView.getQuery())) {
                            filterVouchers(searchView.getQuery().toString());
                        } else {
                            voucherList.clear();
                            voucherList.addAll(voucherListOriginal);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    // --- Hiển thị Dialog Thêm/Sửa ---
    private void showVoucherDialog(VoucherEntity voucherToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_voucher, null);
        builder.setView(dialogView);

        // Ánh xạ View
        ImageView imgPreview = dialogView.findViewById(R.id.img_dialog_voucher);
        EditText edtImageUrl = dialogView.findViewById(R.id.edt_image_url);
        Button btnPreview = dialogView.findViewById(R.id.btn_preview_image);

        EditText edtCode = dialogView.findViewById(R.id.edt_voucher_code);
        // TỐI ƯU: Đảm bảo nhập mã mượt mà bằng cách cho phép nhập thường, chỉ ép kiểu khi validate
        edtCode.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

        EditText edtName = dialogView.findViewById(R.id.edt_voucher_name);
        EditText edtDesc = dialogView.findViewById(R.id.edt_voucher_desc);

        EditText edtVal = dialogView.findViewById(R.id.edt_discount_value);
        EditText edtMinOrder = dialogView.findViewById(R.id.edt_min_order);
        // MỚI: Ánh xạ ô Giảm tối đa
        EditText edtMaxDiscount = dialogView.findViewById(R.id.edt_max_discount);

        // GẮN BỘ FORMAT SỐ (Để nhập 10000 -> 10,000)
        edtVal.addTextChangedListener(new MoneyTextWatcher(edtVal));
        edtMinOrder.addTextChangedListener(new MoneyTextWatcher(edtMinOrder));
        edtMaxDiscount.addTextChangedListener(new MoneyTextWatcher(edtMaxDiscount));


        TextView tvExpiry = dialogView.findViewById(R.id.tv_expiry_date);
        SwitchMaterial switchActive = dialogView.findViewById(R.id.switch_active);
        RadioGroup rgType = dialogView.findViewById(R.id.radio_group_type);
        RadioButton rbPercent = dialogView.findViewById(R.id.radio_percent);
        RadioButton rbAmount = dialogView.findViewById(R.id.radio_amount);

        Button btnSave = dialogView.findViewById(R.id.btn_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete);

        selectedExpiryTimestamp = 0;

        // Preview ảnh
        btnPreview.setOnClickListener(v -> {
            String url = edtImageUrl.getText().toString().trim();
            if (!TextUtils.isEmpty(url)) {
                Glide.with(getContext()).load(url)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(imgPreview);
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập link ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        tvExpiry.setOnClickListener(v -> showDatePicker(tvExpiry));

        switchActive.setOnCheckedChangeListener((buttonView, isChecked) ->
                buttonView.setText(isChecked ? "Trạng thái: Đang hoạt động" : "Trạng thái: Ngừng hoạt động")
        );

        // --- SETUP DATA NẾU LÀ SỬA ---
        if (voucherToEdit != null) {
            btnDelete.setVisibility(View.VISIBLE); // Hiện nút xóa

            edtCode.setText(voucherToEdit.getCode());
            edtCode.setEnabled(false); // Không cho sửa code
            edtName.setText(voucherToEdit.getName());
            edtDesc.setText(voucherToEdit.getDescription());
            edtImageUrl.setText(voucherToEdit.getImageUrl());

            // Format số: bỏ đuôi .0 và thêm dấu phẩy
            edtVal.setText(formatDoubleToString(voucherToEdit.getDiscountValue()));
            edtMinOrder.setText(formatDoubleToString(voucherToEdit.getMinOrderValue()));
            edtMaxDiscount.setText(formatDoubleToString(voucherToEdit.getMaxOrderValue())); // MỚI

            switchActive.setChecked(voucherToEdit.isActive());
            switchActive.setText(voucherToEdit.isActive() ? "Trạng thái: Đang hoạt động" : "Trạng thái: Ngừng hoạt động");

            selectedExpiryTimestamp = voucherToEdit.getExpiryDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvExpiry.setText("Hạn: " + sdf.format(selectedExpiryTimestamp));

            if ("AMOUNT".equals(voucherToEdit.getDiscountType())) {
                rbAmount.setChecked(true);
            } else {
                rbPercent.setChecked(true);
            }

            if (voucherToEdit.getImageUrl() != null && !voucherToEdit.getImageUrl().isEmpty()) {
                Glide.with(getContext()).load(voucherToEdit.getImageUrl()).into(imgPreview);
            }
        } else {
            // NẾU LÀ THÊM MỚI
            btnDelete.setVisibility(View.GONE); // Ẩn nút xóa
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        // XỬ LÝ SỰ KIỆN CÁC NÚT
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Nút Xóa (Trong dialog)
        btnDelete.setOnClickListener(v -> {
            confirmDelete(voucherToEdit, dialog);
        });

        // Nút Lưu - Validate kỹ
        btnSave.setOnClickListener(v -> {
            // VALIDATE VÀ FORMAT TẠI ĐÂY
            String rawCode = edtCode.getText().toString();
            String code = rawCode.replaceAll("\\s+", "").toUpperCase();

            String name = edtName.getText().toString().trim();
            String desc = edtDesc.getText().toString().trim();
            String imgUrl = edtImageUrl.getText().toString().trim();

            // Xóa dấu phẩy format trước khi parse
            String valStr = edtVal.getText().toString().trim().replaceAll("[,.]", "");
            String minOrderStr = edtMinOrder.getText().toString().trim().replaceAll("[,.]", "");
            String maxDiscountStr = edtMaxDiscount.getText().toString().trim().replaceAll("[,.]", ""); // MỚI

            // Validate
            if (TextUtils.isEmpty(code)) {
                edtCode.setError("Vui lòng nhập mã Voucher");
                edtCode.requestFocus();
                return;
            }
            if (code.length() < 3) {
                edtCode.setError("Mã Voucher phải có ít nhất 3 ký tự");
                edtCode.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(name)) {
                edtName.setError("Vui lòng nhập tên chương trình");
                edtName.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(valStr)) {
                edtVal.setError("Vui lòng nhập giá trị giảm");
                edtVal.requestFocus();
                return;
            }

            double discountVal;
            try {
                discountVal = Double.parseDouble(valStr);
            } catch (NumberFormatException e) {
                edtVal.setError("Giá trị giảm không hợp lệ");
                return;
            }

            // Logic check số âm/phần trăm
            if (rbPercent.isChecked()) {
                if (discountVal <= 0 || discountVal > 100) {
                    edtVal.setError("Phần trăm giảm phải từ 1 đến 100");
                    edtVal.requestFocus();
                    return;
                }
            } else {
                if (discountVal <= 0) {
                    edtVal.setError("Số tiền giảm phải lớn hơn 0");
                    edtVal.requestFocus();
                    return;
                }
                if (discountVal > 100000000) {
                    edtVal.setError("Số tiền giảm quá lớn");
                    edtVal.requestFocus();
                    return;
                }
            }

            if (selectedExpiryTimestamp == 0) {
                Toast.makeText(getContext(), "Vui lòng chọn hạn sử dụng", Toast.LENGTH_SHORT).show();
                return;
            }

            if (voucherToEdit == null && selectedExpiryTimestamp < System.currentTimeMillis()) {
                Toast.makeText(getContext(), "Hạn sử dụng phải lớn hơn hiện tại", Toast.LENGTH_SHORT).show();
                return;
            }

            // Xử lý Min Order
            double tempMinOrder = 0;
            if (!minOrderStr.isEmpty()) {
                try {
                    tempMinOrder = Double.parseDouble(minOrderStr);
                } catch (NumberFormatException e) {
                    edtMinOrder.setError("Giá trị đơn tối thiểu không hợp lệ");
                    return;
                }
            }
            double finalMinOrder = tempMinOrder; // Biến final

            // --- XỬ LÝ MAX ORDER VALUE (GIẢM TỐI ĐA) ---
            double tempMaxOrder = 0;
            if (rbPercent.isChecked()) {
                // Chỉ xử lý Max Discount khi chọn loại là %
                if (!maxDiscountStr.isEmpty()) {
                    try {
                        tempMaxOrder = Double.parseDouble(maxDiscountStr);
                    } catch (NumberFormatException e) {
                        edtMaxDiscount.setError("Số liệu không hợp lệ");
                        return;
                    }
                }
            } else {
                // Nếu là giảm tiền mặt (AMOUNT) -> Max Discount = 0
                tempMaxOrder = 0;
            }
            double finalMaxOrder = tempMaxOrder;
            String type = rbPercent.isChecked() ? "PERCENT" : "AMOUNT";
            boolean isActive = switchActive.isChecked();

            ProgressDialog pd = new ProgressDialog(getContext());
            pd.setMessage("Đang lưu Voucher...");
            pd.show();

            // --- KIỂM TRA TRÙNG MÃ (DUPLICATE) ---
            boolean isNewCode = (voucherToEdit == null) || !voucherToEdit.getCode().equals(code);

            if (isNewCode) {
                vouchersRef.whereEqualTo("code", code).get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                pd.dismiss();
                                edtCode.setError("Mã voucher này đã tồn tại!");
                                edtCode.requestFocus();
                            } else {
                                // Truyền thêm finalMaxOrder vào hàm save
                                saveVoucherToFirestore(voucherToEdit, code, name, desc, imgUrl, type, discountVal, finalMinOrder, finalMaxOrder, isActive, pd, dialog);
                            }
                        })
                        .addOnFailureListener(e -> {
                            pd.dismiss();
                            Toast.makeText(getContext(), "Lỗi kiểm tra trùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                // Truyền thêm finalMaxOrder vào hàm save
                saveVoucherToFirestore(voucherToEdit, code, name, desc, imgUrl, type, discountVal, finalMinOrder, finalMaxOrder, isActive, pd, dialog);            }
        });
    }

    // --- Hàm tách logic Lưu để code gọn hơn ---
    private void saveVoucherToFirestore(VoucherEntity currentVoucher, String code, String name, String desc,
                                        String imgUrl, String type, double discountVal, double minOrder,
                                        double maxOrderVal, // <--- THÊM THAM SỐ NÀY
                                        boolean isActive, ProgressDialog pd, AlertDialog dialog) {

        String voucherId = (currentVoucher == null) ? vouchersRef.document().getId() : currentVoucher.getId();

        VoucherEntity newVoucher = new VoucherEntity();
        newVoucher.setId(voucherId);
        newVoucher.setCode(code);
        newVoucher.setName(name);
        newVoucher.setDescription(desc);
        newVoucher.setImageUrl(imgUrl);
        newVoucher.setDiscountType(type);
        newVoucher.setDiscountValue(discountVal);
        newVoucher.setMinOrderValue(minOrder);
        newVoucher.setMaxOrderValue(maxOrderVal); // <--- LƯU MAX ORDER VALUE
        newVoucher.setExpiryDate(selectedExpiryTimestamp);
        newVoucher.setActive(isActive);

        vouchersRef.document(voucherId).set(newVoucher)
                .addOnSuccessListener(unused -> {
                    pd.dismiss();
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Lưu thành công!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Helper: Format số bỏ đuôi .0 và thêm dấu phẩy (Dùng để hiển thị lên dialog)
    private String formatDoubleToString(double value) {
        if (value == 0) return "0";
        // Dùng String.format để thêm dấu phẩy phân cách hàng nghìn
        if (value == (long) value) {
            return String.format(Locale.US, "%,d", (long) value);
        } else {
            return String.format(Locale.US, "%,.2f", value); // Hoặc giữ nguyên logic cũ nếu muốn
        }
    }

    // Logic Xóa Voucher
    private void confirmDelete(VoucherEntity voucher, AlertDialog parentDialog) {
        if (voucher == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn chắc chắn muốn xóa voucher [" + voucher.getCode() + "] không?")
                .setPositiveButton("Xóa", (d, w) -> {
                    vouchersRef.document(voucher.getId()).delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(getContext(), "Đã xóa voucher", Toast.LENGTH_SHORT).show();
                                if (parentDialog != null) parentDialog.dismiss();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDatePicker(TextView tvDisplay) {
        Calendar calendar = Calendar.getInstance();
        if (selectedExpiryTimestamp != 0) {
            calendar.setTimeInMillis(selectedExpiryTimestamp);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth, 23, 59, 59);
                    selectedExpiryTimestamp = calendar.getTimeInMillis();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    tvDisplay.setText("Hạn: " + sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    // Lớp hỗ trợ format tiền tệ khi nhập liệu (VD: gõ 100000 -> hiển thị 100,000)
    private class MoneyTextWatcher implements TextWatcher {
        private final EditText editText;

        public MoneyTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            editText.removeTextChangedListener(this); // Tạm ngắt listener

            try {
                String originalString = s.toString();
                if (!originalString.isEmpty()) {
                    // Xóa các dấu phân cách cũ đi để lấy số thô
                    String cleanString = originalString.replaceAll("[,.]", "");

                    double parsed = Double.parseDouble(cleanString);
                    // Định dạng số nguyên có dấu phẩy phân cách
                    String formatted = String.format(Locale.US, "%,d", (long) parsed);

                    // Set lại text
                    editText.setText(formatted);
                    // Đưa con trỏ về cuối dòng
                    editText.setSelection(formatted.length());
                }
            } catch (NumberFormatException e) {
                // e.printStackTrace();
            }

            editText.addTextChangedListener(this); // Bật lại listener
        }
    }
}
