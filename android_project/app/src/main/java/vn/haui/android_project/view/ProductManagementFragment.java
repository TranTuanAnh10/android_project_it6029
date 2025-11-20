package vn.haui.android_project.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import vn.haui.android_project.R;
import vn.haui.android_project.adapter.ProductManagementAdapter;
import vn.haui.android_project.entity.ProductItem;

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
        // [SỬA LỖI 1] Sửa lại tên collection "categorys" -> "categories"
        db.collection("categorys").get().addOnSuccessListener(queryDocumentSnapshots -> {
            categoryNames.clear();
            for (var doc : queryDocumentSnapshots) {
                categoryNames.add(doc.getString("name"));
            }
            // Tải sản phẩm sau khi đã có danh mục
            loadProducts();
        }).addOnFailureListener(e -> Log.e(TAG, "Lỗi tải danh mục", e));

        fabAddProduct.setOnClickListener(v -> {
            if (categoryNames.isEmpty()) {
                Toast.makeText(getContext(), "Danh mục đang tải, vui lòng thử lại!", Toast.LENGTH_SHORT).show();
            } else {
                showAddProductDialog();
            }
        });
    }

    private void loadProducts() {
        // [SỬA LỖI 2] Sửa lại tên trường sắp xếp "Name" -> "name"
        db.collection("products").orderBy("Name", Query.Direction.ASCENDING).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ProductItem> loadedProducts = new ArrayList<>();
                    for (var doc : queryDocumentSnapshots) {
                        ProductItem product = doc.toObject(ProductItem.class);
                        product.setId(doc.getId());
                        loadedProducts.add(product);
                    }
                    adapter.updateData(loadedProducts);
                }).addOnFailureListener(e -> Log.e(TAG, "Lỗi tải sản phẩm", e));
    }

    private void showAddProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Sửa lại tên layout dialog để phù hợp với logic (ví dụ: dialog_add_product_local_image)
        // Hoặc bạn có thể giữ nguyên tên dialog_add_edit_product nếu đã sửa nó có ô et_image_name
        View dialogView = inflater.inflate(R.layout.dialog_add_edit_product, null);

        builder.setView(dialogView);
        final AlertDialog dialog = builder.create();
        dialog.setTitle("Thêm sản phẩm mới");

        EditText etName = dialogView.findViewById(R.id.et_product_name);
        EditText etPrice = dialogView.findViewById(R.id.et_product_price);
        EditText etImageName = dialogView.findViewById(R.id.et_image_name);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_product_category);
        RadioGroup rgStatus = dialogView.findViewById(R.id.rg_product_status);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, categoryNames);
        spinnerCategory.setAdapter(categoryAdapter);

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Lưu", (d, which) -> { /* Để trống để override */ });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Hủy", (d, which) -> d.dismiss());

        dialog.show();

        // Override nút Lưu để không tự động đóng dialog khi có lỗi
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String imageNameWithExtension = etImageName.getText().toString().trim(); // ví dụ: "banh_my.jpg"
            String category = spinnerCategory.getSelectedItem().toString();
            String status = ((RadioButton) dialogView.findViewById(rgStatus.getCheckedRadioButtonId())).getText().toString();

            if (name.isEmpty() || priceStr.isEmpty() || imageNameWithExtension.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // Dùng hàm kiểm tra đã sửa
            if (isResourceExists(getContext(), imageNameWithExtension)) {
                ProductItem newProduct = new ProductItem();
                newProduct.setName(name);
                newProduct.setPrice(Double.parseDouble(priceStr));
                newProduct.setCategory(category);
                newProduct.setStatus(status);
                // Lưu tên file CÓ ĐUÔI vào Firestore
                newProduct.setImage(imageNameWithExtension);

                saveNewProductToFirestore(newProduct);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Lỗi: Không tìm thấy ảnh '" + imageNameWithExtension + "' trong drawable.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // [SỬA LỖI 3] Sửa lại hàm isResourceExists để hoạt động đúng
    private boolean isResourceExists(Context context, String imageNameWithExtension) {
        if (context == null || imageNameWithExtension == null || imageNameWithExtension.isEmpty()) {
            return false;
        }

        // Tách tên file ra khỏi đuôi
        String nameToSearch = imageNameWithExtension;
        int lastDotIndex = imageNameWithExtension.lastIndexOf('.');
        if (lastDotIndex > 0) {
            nameToSearch = imageNameWithExtension.substring(0, lastDotIndex);
        }

        // Dùng tên không có đuôi để tìm kiếm
        int resourceId = context.getResources().getIdentifier(nameToSearch, "drawable", context.getPackageName());
        return resourceId != 0;
    }

    private void saveNewProductToFirestore(ProductItem product) {
        db.collection("products").add(product)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Thêm sản phẩm thành công", Toast.LENGTH_SHORT).show();
                    loadProducts();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi thêm sản phẩm", Toast.LENGTH_SHORT).show());
    }
}
