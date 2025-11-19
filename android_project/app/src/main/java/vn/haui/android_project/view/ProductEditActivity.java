package vn.haui.android_project.view;

import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
// Bỏ các import không cần thiết nữa
// import android.app.Activity;
// import android.content.Intent;
// import android.net.Uri;
// import com.bumptech.glide.Glide;
// import com.google.firebase.storage.FirebaseStorage;
// import com.google.firebase.storage.StorageReference;
// import androidx.activity.result.ActivityResultLauncher;
// import androidx.activity.result.contract.ActivityResultContracts;

import java.util.ArrayList;
import java.util.List;
// import java.util.UUID; // Bỏ UUID

import vn.haui.android_project.R;
import vn.haui.android_project.entity.CategoryItem;
import vn.haui.android_project.entity.ProductItem;

public class ProductEditActivity extends AppCompatActivity {

    private static final String TAG = "ProductEditActivity";
    private ImageView ivProductImage;
    private Button btnChangeImage, btnUpdate, btnDelete;
    private EditText etName, etPrice;
    private Spinner spinnerCategory;
    private RadioGroup rgStatus;

    private FirebaseFirestore db;
    // private FirebaseStorage storage; // Bỏ storage
    private String productId;
    private ProductItem currentProduct;

    private List<CategoryItem> categoryList = new ArrayList<>();
    private List<String> categoryNames = new ArrayList<>();

    // Bỏ các biến liên quan đến upload ảnh
    // private Uri selectedImageUri;
    // private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_edit);

        db = FirebaseFirestore.getInstance();
        // Bỏ storage = FirebaseStorage.getInstance();

        productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId == null || productId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        // Bỏ registerImagePicker();
        loadCategoriesAndProductData();

        // Thay đổi logic của nút "Thay đổi ảnh"
        btnChangeImage.setOnClickListener(v -> showChangeImageDialog());
        btnUpdate.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void initViews() {
        ivProductImage = findViewById(R.id.iv_edit_product_image);
        btnChangeImage = findViewById(R.id.btn_edit_change_image);
        etName = findViewById(R.id.et_edit_product_name);
        etPrice = findViewById(R.id.et_edit_product_price);
        spinnerCategory = findViewById(R.id.spinner_edit_product_category);
        rgStatus = findViewById(R.id.rg_edit_product_status);
        btnUpdate = findViewById(R.id.btn_update_product);
        btnDelete = findViewById(R.id.btn_delete_product);
    }

    private void loadCategoriesAndProductData() {
        db.collection("categorys").get().addOnSuccessListener(queryDocumentSnapshots -> {
            categoryList.clear();
            categoryNames.clear();
            for (var doc : queryDocumentSnapshots) {
                CategoryItem category = doc.toObject(CategoryItem.class);
                categoryList.add(category);
                categoryNames.add(category.getName());
            }
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryNames);
            spinnerCategory.setAdapter(categoryAdapter);
            loadProductData();
        }).addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải danh mục", Toast.LENGTH_SHORT).show());
    }

    private void loadProductData() {
        db.collection("products").document(productId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentProduct = documentSnapshot.toObject(ProductItem.class);
                        if (currentProduct != null) {
                            currentProduct.setId(documentSnapshot.getId());
                            displayProductData();
                        }
                    } else {
                        Toast.makeText(this, "Sản phẩm không còn tồn tại.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu sản phẩm.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayProductData() {
        etName.setText(currentProduct.getName());
        etPrice.setText(String.valueOf(currentProduct.getPrice()));

        // [SỬA 1] Load ảnh từ drawable bằng tên
        String imageName = currentProduct.getImage();
        int index = imageName.lastIndexOf('.');
        if (index != -1) {
            imageName = imageName.substring(0, index);
        }
        int imageId = getResources().getIdentifier(imageName, "drawable", getPackageName());
        if (imageId != 0) {
            ivProductImage.setImageResource(imageId);
        } else {
            ivProductImage.setImageResource(R.drawable.img_placeholder); // Ảnh mặc định nếu không tìm thấy
        }

        int categoryPosition = categoryNames.indexOf(currentProduct.getCategory());
        spinnerCategory.setSelection(Math.max(categoryPosition, 0));

        if ("Hết hàng".equalsIgnoreCase(currentProduct.getStatus())) {
            rgStatus.check(R.id.rb_edit_status_out_of_stock);
        } else {
            rgStatus.check(R.id.rb_edit_status_in_stock);
        }
    }

    // [SỬA 2] Tạo dialog để người dùng nhập tên ảnh mới
    private void showChangeImageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thay đổi ảnh sản phẩm");

        // Tạo một EditText cho người dùng nhập
        final EditText input = new EditText(this);
        input.setHint("Nhập tên ảnh mới từ drawable");
        input.setText(currentProduct.getImage()); // Hiển thị sẵn tên ảnh cũ
        builder.setView(input);

        // Thiết lập các nút
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newImageName = input.getText().toString().trim();
            if (newImageName.isEmpty()) {
                Toast.makeText(this, "Tên ảnh không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra xem ảnh mới có tồn tại không
            if (isResourceExists(this, newImageName)) {
                // Cập nhật ảnh trong UI và trong đối tượng currentProduct
                int imageId = getResources().getIdentifier(newImageName, "drawable", getPackageName());
                ivProductImage.setImageResource(imageId);
                currentProduct.setImage(newImageName); // Cập nhật tên ảnh mới vào đối tượng
                Toast.makeText(this, "Đã thay đổi ảnh. Nhấn 'Cập nhật' để lưu.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Lỗi: Không tìm thấy ảnh '" + newImageName + "' trong drawable.", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }


    // [SỬA 3] Đơn giản hóa hàm saveChanges
    private void saveChanges() {
        String name = etName.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String status = ((RadioButton) findViewById(rgStatus.getCheckedRadioButtonId())).getText().toString();

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cập nhật thông tin vào đối tượng currentProduct
        currentProduct.setName(name);
        currentProduct.setPrice(Double.parseDouble(priceStr));
        currentProduct.setCategory(category);
        currentProduct.setStatus(status);
        // Tên ảnh đã được cập nhật từ hàm showChangeImageDialog() nếu người dùng thay đổi

        // Lưu toàn bộ đối tượng vào Firestore
        db.collection("products").document(productId).set(currentProduct)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish(); // Quay về màn hình danh sách
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa sản phẩm '" + currentProduct.getName() + "' không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> deleteProduct())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteProduct() {
        db.collection("products").document(productId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                    finish(); // Quay về màn hình danh sách
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi xóa sản phẩm", Toast.LENGTH_SHORT).show());
    }

    // Hàm tiện ích để kiểm tra sự tồn tại của tài nguyên drawable
    private boolean isResourceExists(Context context, String resourceName) {
        int resourceId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
        return resourceId != 0;
    }
}
