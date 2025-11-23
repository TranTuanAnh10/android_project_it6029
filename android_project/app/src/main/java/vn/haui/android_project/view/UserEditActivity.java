package vn.haui.android_project.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserEntity;

public class UserEditActivity extends AppCompatActivity {

    private ImageView imgBack, imgAvatar;
    private TextInputEditText edtName, edtPhone, edtEmail;

    // Thay Spinner bằng RadioGroup và các RadioButton
    private RadioGroup rgRole;
    private RadioButton rbUser, rbEmployee, rbShipper, rbAdmin;

    private Button btnSaveChanges;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_edit);

        db = FirebaseFirestore.getInstance();

        userId = getIntent().getStringExtra("USER_ID");

        initViews();

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ID người dùng.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadUserData();
        setupListeners();
    }

    private void initViews() {
        imgBack = findViewById(R.id.img_back);
        edtName = findViewById(R.id.edt_name);
        edtPhone = findViewById(R.id.edt_phone);
        edtEmail = findViewById(R.id.edt_email);

        // Ánh xạ RadioGroup và RadioButton
        rgRole = findViewById(R.id.rg_role);
        rbUser = findViewById(R.id.rb_user);
        rbEmployee = findViewById(R.id.rb_employee);
        rbShipper = findViewById(R.id.rb_shipper);
        rbAdmin = findViewById(R.id.rb_admin);

        btnSaveChanges = findViewById(R.id.btn_save_changes);
    }


    private void loadUserData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserEntity user = documentSnapshot.toObject(UserEntity.class);
                        if (user != null) {
                            // 1. Hiển thị thông tin
                            if (user.getName() != null) {
                                edtName.setText(user.getName());
                            }
                            edtEmail.setText(user.getEmail());
                            String phone = user.getPhoneNumber();
                            edtPhone.setText(phone != null ? phone : "Chưa cập nhật");

                            // Load ảnh nếu có
                            /*
                            if (user.getImage() != null && !user.getImage().isEmpty()) {
                                Glide.with(this).load(user.getImage()).into(imgAvatar);
                            }
                            */

                            // 2. Chọn RadioButton tương ứng với Role hiện tại
                            String currentRole = user.getRole();
                            if (currentRole != null) {
                                String roleLower = currentRole.toLowerCase();
                                if (roleLower.contains("admin")) {
                                    rbAdmin.setChecked(true);
                                } else if (roleLower.contains("shipper")) {
                                    rbShipper.setChecked(true);
                                } else if (roleLower.contains("employee")) {
                                    rbEmployee.setChecked(true);
                                } else {
                                    rbUser.setChecked(true); // Mặc định hoặc là User
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Người dùng không tồn tại.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupListeners() {
        imgBack.setOnClickListener(v -> finish());
        btnSaveChanges.setOnClickListener(v -> saveRoleChanges());
    }

    private void saveRoleChanges() {
        String selectedRole = "user"; // Mặc định

        // Kiểm tra xem nút nào đang được tích
        if (rbAdmin.isChecked()) {
            selectedRole = "admin";
        } else if (rbShipper.isChecked()) {
            selectedRole = "shipper";
        } else if (rbEmployee.isChecked()) {
            selectedRole = "employee";
        } else if (rbUser.isChecked()) {
            selectedRole = "user";
        }

        // Cập nhật Firestore
        String finalRole = selectedRole;
        db.collection("users").document(userId)
                .update("role", finalRole)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật vai trò [" + finalRole + "] thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
