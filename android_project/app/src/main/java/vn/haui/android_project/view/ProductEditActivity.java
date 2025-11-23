package vn.haui.android_project.view;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.android_project.R;

public class ProductEditActivity extends AppCompatActivity {

    // --- Khai báo View ---
    private TextInputEditText etName, etPrice, etDescription, etImageUrl;
    private ImageView imgPreview, btnLoadPreview, imgBack; // imgBack là nút back trên toolbar custom
    private Spinner spinnerCategory;
    private MaterialSwitch switchStatus;
    private Button btnSave, btnDelete;
    private Toolbar toolbar;

    // --- Firebase & Data ---
    private FirebaseFirestore db;
    private String productId;
    private List<String> categoryList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_edit);

        // 1. Lấy ID sản phẩm từ Intent truyền sang
        productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        // 2. Khởi tạo View và Sự kiện
        initViews();
        setupListeners();

        // 3. Tải dữ liệu (Danh mục trước -> Chi tiết sản phẩm sau)
        loadCategoriesAndProductData();
    }

    private void initViews() {
        // Ánh xạ ID khớp với file XML activity_product_edit (kiểu User Edit)
        toolbar = findViewById(R.id.toolbar_edit_product);
        imgBack = findViewById(R.id.img_back); // Nút back trên Toolbar

        etName = findViewById(R.id.et_edit_name);
        etPrice = findViewById(R.id.et_edit_price);
        etDescription = findViewById(R.id.et_edit_description);
        etImageUrl = findViewById(R.id.et_edit_image_url);

        imgPreview = findViewById(R.id.img_edit_preview);
        btnLoadPreview = findViewById(R.id.btn_edit_load_preview);

        spinnerCategory = findViewById(R.id.spinner_edit_category);
        switchStatus = findViewById(R.id.switch_edit_status);

        btnSave = findViewById(R.id.btn_save_changes);
        btnDelete = findViewById(R.id.btn_delete_product);

        // Kích hoạt bộ formatter tiền tệ cho ô Giá
        addCurrencyFormatter(etPrice);
    }

    private void setupListeners() {
        // Nút Back trên Toolbar
        imgBack.setOnClickListener(v -> finish());
        // Hoặc click vào cả toolbar để back nếu muốn
        toolbar.setNavigationOnClickListener(v -> finish());

        // Nút load ảnh preview bên cạnh ô nhập link
        btnLoadPreview.setOnClickListener(v -> {
            String url = etImageUrl.getText().toString().trim();
            loadPreviewImage(url);
        });

        // Nút Lưu thay đổi
        btnSave.setOnClickListener(v -> saveProductChanges());

        // Nút Xóa sản phẩm
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());

        // [MỚI] Xử lý thay đổi trạng thái Switch
        switchStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateSwitchVisuals(isChecked);
        });
    }

    // --- LOAD DỮ LIỆU ---
    private void loadCategoriesAndProductData() {
        // Bước 1: Load danh sách Category để đổ vào Spinner trước
        db.collection("categorys").get().addOnSuccessListener(queryDocumentSnapshots -> {
            categoryList.clear();
            for (var doc : queryDocumentSnapshots) {
                String name = doc.getString("name");
                if (name == null) name = doc.getString("Name");
                if (name != null) categoryList.add(name);
            }

            // Setup Adapter cho Spinner
            categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryList);
            spinnerCategory.setAdapter(categoryAdapter);

            // Bước 2: Sau khi có danh mục thì load chi tiết sản phẩm
            loadProductDetails();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi tải danh mục", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadProductDetails() {
        db.collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // 1. Tên
                        String name = doc.getString("Name");
                        etName.setText(name != null ? name : "");

                        // 2. Giá (Format có dấu phẩy)
                        Double price = doc.getDouble("Price");
                        if (price != null) {
                            String formattedPrice = NumberFormat.getInstance(Locale.US).format(price);
                            etPrice.setText(formattedPrice);
                        }

                        // 3. Ảnh
                        String image = doc.getString("Image");
                        etImageUrl.setText(image != null ? image : "");
                        loadPreviewImage(image);

                        // 4. Mô tả
                        String desc = doc.getString("Description");
                        etDescription.setText(desc != null ? desc : "");

                        // 5. Danh mục (Chọn đúng item trong spinner)
                        String category = doc.getString("Category");
                        if (category != null && categoryAdapter != null) {
                            int position = categoryAdapter.getPosition(category);
                            if (position >= 0) spinnerCategory.setSelection(position);
                        }

                        // 6. Trạng thái (Map từ string sang boolean switch)
                        String status = doc.getString("status");
                        if (status == null) status = doc.getString("Status");

                        boolean isAvailable = "available".equalsIgnoreCase(status) || "Còn hàng".equalsIgnoreCase(status);
                        switchStatus.setChecked(isAvailable);

                        // [MỚI] Cập nhật màu sắc ngay lập tức sau khi setChecked
                        updateSwitchVisuals(isAvailable);
                    } else {
                        Toast.makeText(this, "Sản phẩm không tồn tại", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // --- CẬP NHẬT SẢN PHẨM ---
    private void saveProductChanges() {
        String name = etName.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String imageUrl = etImageUrl.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        String category = "";
        if (spinnerCategory.getSelectedItem() != null) {
            category = spinnerCategory.getSelectedItem().toString();
        }

        // Logic trạng thái: Bật -> available, Tắt -> unavailable
        String status = switchStatus.isChecked() ? "available" : "unavailable";

        // Validate cơ bản
        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Tên và Giá không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        // Xử lý giá: Xóa dấu phẩy trước khi parse sang Double
        double price = 0;
        try {
            String cleanPrice = priceStr.replaceAll("[,.]", "");
            price = Double.parseDouble(cleanPrice);
        } catch (NumberFormatException e) {
            etPrice.setError("Giá không hợp lệ");
            return;
        }

        // Tạo Map dữ liệu để cập nhật
        Map<String, Object> updates = new HashMap<>();

        // === [QUAN TRỌNG] Cập nhật luôn ID vào field để đồng bộ ===
        updates.put("id", productId);
        // ==========================================================

        updates.put("Name", name);
        updates.put("Price", price);
        updates.put("Image", imageUrl);
        updates.put("Description", desc);
        updates.put("Category", category);
        updates.put("status", status);
        // Giữ nguyên rate cũ nếu không muốn sửa, hoặc put vào nếu cần

        db.collection("products").document(productId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Đóng màn hình quay về danh sách
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // --- XÓA SẢN PHẨM ---
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa sản phẩm này không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteProduct())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteProduct() {
        db.collection("products").document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // --- HÀM TIỆN ÍCH ---

    // Load ảnh dùng Glide + UserAgent để tránh lỗi 403
    private void loadPreviewImage(String url) {
        if (url == null || url.isEmpty()) return;

        GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                .build());

        Glide.with(this)
                .load(glideUrl)
                .override(600, 600)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(imgPreview);
    }

    // Formatter tiền tệ (Tự động thêm dấu phẩy khi gõ)
    private void addCurrencyFormatter(TextInputEditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    editText.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[,.]", "");

                    if (!cleanString.isEmpty()) {
                        try {
                            double parsed = Double.parseDouble(cleanString);
                            String formatted = NumberFormat.getInstance(Locale.US).format(parsed);
                            current = formatted;
                            editText.setText(formatted);
                            editText.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                        }
                    } else {
                        current = "";
                        editText.setText("");
                    }
                    editText.addTextChangedListener(this);
                }
            }
        });
    }

    // Hàm đổi màu Switch: On -> Xanh, Off -> Xám/Trắng
    private void updateSwitchVisuals(boolean isChecked) {
        if (isChecked) {
            // KHI BẬT (ON):
            // 1. Track (thanh trượt) màu xanh nhạt hoặc xanh đậm tùy ý
            switchStatus.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

            // 2. Thumb (nút tròn) màu trắng (để nổi bật trên nền xanh)
            switchStatus.setThumbTintList(ColorStateList.valueOf(Color.WHITE));

            switchStatus.setText("Còn hàng");
        } else {
            // KHI TẮT (OFF):
            // 1. Track màu xám nhạt
            switchStatus.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));

            // 2. Thumb màu xám đậm hơn chút hoặc trắng
            switchStatus.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#BDBDBD")));

            switchStatus.setText("Hết hàng");
        }
    }


}
