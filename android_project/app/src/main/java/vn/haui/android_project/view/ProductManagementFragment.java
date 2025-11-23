package vn.haui.android_project.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.ProductManagementAdapter;
import vn.haui.android_project.entity.ProductItem;

import android.content.res.ColorStateList;

public class ProductManagementFragment extends Fragment {

    private static final String TAG = "ProductManagement";
    private RecyclerView recyclerView;
    private FloatingActionButton fabAddProduct;
    private ProductManagementAdapter adapter;
    private List<ProductItem> productList = new ArrayList<>();
    private List<String> categoryNames = new ArrayList<>();

    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_management, container, false);
        recyclerView = view.findViewById(R.id.recycler_view_products);
        fabAddProduct = view.findViewById(R.id.fab_add_product);
        setupRecyclerView();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCategoriesAndProducts();
    }

    private void setupRecyclerView() {
        adapter = new ProductManagementAdapter(getContext(), productList, product -> {
            Intent intent = new Intent(getActivity(), ProductEditActivity.class);
            intent.putExtra("PRODUCT_ID", product.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadCategoriesAndProducts() {
        Log.d(TAG, "Đang bắt đầu tải category...");
        // Lưu ý: Kiểm tra tên collection là 'categories' hay 'categorys' trong DB của bạn
        db.collection("categorys").get().addOnSuccessListener(queryDocumentSnapshots -> {
            categoryNames.clear();

            for (var doc : queryDocumentSnapshots) {
                String catName = doc.getString("name");
                if (catName == null) {
                    catName = doc.getString("Name"); // Thử trường hợp viết hoa
                }
                if (catName != null) {
                    categoryNames.add(catName);
                }
            }
            // Sau khi load xong danh mục mới load sản phẩm
            loadProducts();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi tải danh mục: ", e);
            Toast.makeText(getContext(), "Lỗi kết nối DB: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });

        fabAddProduct.setOnClickListener(v -> {
            if (categoryNames.isEmpty()) {
                Toast.makeText(getContext(), "Đang tải danh mục, vui lòng chờ...", Toast.LENGTH_SHORT).show();
                loadCategoriesAndProducts();
            } else {
                showAddProductDialog();
            }
        });
    }

    // --- PHẦN 1: LOAD SẢN PHẨM VỚI FIELD VIẾT HOA/THƯỜNG LỘN XỘN ---
    private void loadProducts() {
        db.collection("products")
                .orderBy("Name", Query.Direction.ASCENDING) // Sắp xếp theo Name (Hoa)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 1. Xóa list cũ để cập nhật mới hoàn toàn
                    productList.clear();

                    for (var doc : queryDocumentSnapshots) {
                        try {
                            ProductItem product = new ProductItem();
                            product.setId(doc.getId());

                            // --- ÁNH XẠ THỦ CÔNG ĐỂ KHỚP VỚI DB ---

                            // Name (Viết hoa)
                            String name = doc.getString("Name");
                            product.setName(name != null ? name : "No Name");

                            // Price (Viết hoa)
                            Double price = doc.getDouble("Price");
                            product.setPrice(price != null ? price : 0.0);

                            // Image (Viết hoa)
                            String image = doc.getString("Image");
                            product.setImage(image != null ? image : "");

                            // Category (Viết hoa)
                            String category = doc.getString("Category");
                            product.setCategory(category != null ? category : "");

                            // rate (Viết thường - như bạn mô tả)
                            Double rate = doc.getDouble("rate");
                            // Nếu class ProductItem có setRate thì bỏ comment dòng dưới
                            // product.setRate(rate != null ? rate : 5.0);

                            // Description (Viết hoa)
                            String desc = doc.getString("Description");
                            // product.setDescription(desc);

                            // status (Thường là viết thường, kiểm tra cả 2 cho chắc)
                            String status = doc.getString("status");
                            if (status == null) status = doc.getString("Status");
                            product.setStatus(status != null ? status : "available");

                            productList.add(product);
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi parse sản phẩm: " + doc.getId(), e);
                        }
                    }

                    // 2. Thông báo Adapter cập nhật lại
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi tải sản phẩm", e));
    }

    private void showAddProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_edit_product, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // --- Ánh xạ View ---
        TextInputEditText etName = dialogView.findViewById(R.id.et_product_name);
        TextInputEditText etPrice = dialogView.findViewById(R.id.et_product_price);
        TextInputEditText etImageUrl = dialogView.findViewById(R.id.et_image_url);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_product_description);

        ImageView btnLoadPreview = dialogView.findViewById(R.id.btn_load_preview);
        ImageView imgPreview = dialogView.findViewById(R.id.img_preview);

        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_product_category);
        MaterialSwitch switchStatus = dialogView.findViewById(R.id.switch_product_status);
        Button btnSave = dialogView.findViewById(R.id.btn_save_dialog);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_dialog);

        // --- 1. KÍCH HOẠT FORMAT TIỀN TỆ (Thêm dấu phẩy khi nhập) ---
        addCurrencyFormatter(etPrice);

        // --- 2. XỬ LÝ LOAD ẢNH (Code chuẩn) ---
        btnLoadPreview.setOnClickListener(v -> {
            String rawUrl = etImageUrl.getText().toString().trim();
            if (rawUrl.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập URL ảnh", Toast.LENGTH_SHORT).show();
                return;
            }

            // Reset giao diện ảnh
            imgPreview.setImageResource(android.R.drawable.ic_menu_gallery);
            imgPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);

            Glide.with(requireContext())
                    .load(rawUrl)
                    .override(600, 600)
                    .timeout(30000)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "❌ Load ảnh thất bại: " + (e != null ? e.getMessage() : "Unknown"));
                            Toast.makeText(getContext(), "Không tải được ảnh", Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            // Reset padding khi ảnh thật đã lên
                            imgPreview.post(() -> {
                                imgPreview.setPadding(0, 0, 0, 0);
                                imgPreview.setColorFilter(null);
                                imgPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                imgPreview.setImageDrawable(resource);
                            });
                            return true;
                        }
                    })
                    .into(imgPreview);
        });

        // --- Xử lý Switch ---
        //1. Set trạng thái màu sắc và chữ ban đầu ngay khi mở dialog
        updateSwitchVisuals(switchStatus, switchStatus.isChecked());

        // 2. Lắng nghe sự kiện click để đổi màu và chữ
        switchStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateSwitchVisuals(switchStatus, isChecked);
        });


        // --- Setup Spinner ---
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, categoryNames);
        spinnerCategory.setAdapter(categoryAdapter);

        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // --- Xử lý nút LƯU ---
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String imageUrl = etImageUrl.getText().toString().trim();
            // [MỚI] Lấy dữ liệu mô tả
            String description = etDescription.getText().toString().trim();
            if (description.isEmpty()) {
                description = "Chưa có mô tả"; // Giá trị mặc định nếu người dùng để trống
            }

            String category = "";
            if (spinnerCategory.getSelectedItem() != null) {
                category = spinnerCategory.getSelectedItem().toString();
            }

            String status = switchStatus.isChecked() ? "available" : "unavailable";

            if (name.isEmpty() || priceStr.isEmpty() || imageUrl.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            double price;
            try {
                // QUAN TRỌNG: Xóa dấu phẩy trước khi parse số (do formatter thêm vào)
                String cleanPrice = priceStr.replaceAll("[,.]", "");
                price = Double.parseDouble(cleanPrice);
            } catch (NumberFormatException e) {
                etPrice.setError("Giá không hợp lệ");
                return;
            }

            ProductItem newProduct = new ProductItem();
            newProduct.setName(name);
            newProduct.setPrice(price);
            newProduct.setCategory(category);
            newProduct.setStatus(status);
            newProduct.setImage(imageUrl);
            newProduct.setDescription(description);
            saveNewProductToFirestore(newProduct);
            dialog.dismiss();
        });
    }

    // --- PHẦN 2: LƯU SẢN PHẨM MỚI THEO ĐÚNG TÊN FIELD CỦA BẠN ---
    private void saveNewProductToFirestore(ProductItem product) {
        // 1. Tạo một reference rỗng để lấy ID tự sinh trước
        com.google.firebase.firestore.DocumentReference newRef = db.collection("products").document();
        String newId = newRef.getId(); // Đây là ID mới (ví dụ: 7hJk9...)

        // 2. Đưa dữ liệu vào Map
        Map<String, Object> data = new HashMap<>();

        // === QUAN TRỌNG: Lưu ID vào trong data luôn ===
        data.put("id", newId);
        // ==============================================

        data.put("Name", product.getName());
        data.put("Price", product.getPrice());
        data.put("Image", product.getImage());
        data.put("Category", product.getCategory());
        data.put("Description", product.getDescription());
        data.put("rate", 5.0);
        data.put("status", product.getStatus());

        // 3. Dùng lệnh set() thay vì add() để lưu vào đúng cái ID vừa tạo
        newRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Thêm sản phẩm thành công", Toast.LENGTH_SHORT).show();
                    loadProducts();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi thêm sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    // --- PHẦN 3: HÀM FORMAT TIỀN TỆ (TỰ ĐỘNG THÊM DẤU PHẨY) ---
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

                    // Xóa các ký tự không phải số để format lại
                    String cleanString = s.toString().replaceAll("[,.]", "");

                    if (!cleanString.isEmpty()) {
                        try {
                            double parsed = Double.parseDouble(cleanString);
                            // Format số (ví dụ: 1,200,000)
                            String formatted = NumberFormat.getInstance(Locale.US).format(parsed);
                            current = formatted;
                            editText.setText(formatted);
                            editText.setSelection(formatted.length()); // Đặt con trỏ về cuối
                        } catch (NumberFormatException e) {
                            // Ignore
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

    // --- HÀM XỬ LÝ MÀU SWITCH (Đã sửa) ---
    // Phải truyền tham số 'MaterialSwitch sw' vào vì switch nằm trong Dialog
    private void updateSwitchVisuals(MaterialSwitch sw, boolean isChecked) {
        if (isChecked) {
            // KHI BẬT (ON):
            // 1. Track (thanh trượt) màu xanh
            sw.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

            // 2. Thumb (nút tròn) màu trắng
            sw.setThumbTintList(ColorStateList.valueOf(Color.WHITE));

            sw.setText("Còn hàng");
        } else {
            // KHI TẮT (OFF):
            // 1. Track màu xám nhạt
            sw.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));

            // 2. Thumb màu xám đậm hơn chút
            sw.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#BDBDBD")));

            sw.setText("Hết hàng");
        }
    }

}
