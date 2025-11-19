package vn.haui.android_project.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserEntity;

public class UserEditActivity extends AppCompatActivity {

    private TextView tvFullName, tvEmail, tvPhoneNumber, tvCreationDate;
    private RadioGroup rgRole;
    private RadioButton rbAdmin, rbEmployee, rbCustomer;
    private Button btnSaveChanges;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_edit);

        // Ánh xạ View
        tvFullName = findViewById(R.id.tv_edit_full_name);
        tvEmail = findViewById(R.id.tv_edit_email);
        tvPhoneNumber = findViewById(R.id.tv_edit_phone_number);
        tvCreationDate = findViewById(R.id.tv_edit_creation_date);
        rgRole = findViewById(R.id.rg_edit_role);
        rbAdmin = findViewById(R.id.rb_role_admin);
        rbEmployee = findViewById(R.id.rb_role_employee);
        rbCustomer = findViewById(R.id.rb_role_customer);
        btnSaveChanges = findViewById(R.id.btn_save_role_changes);

        db = FirebaseFirestore.getInstance();

        // Lấy userId được truyền từ UserManagementFragment
        userId = getIntent().getStringExtra("USER_ID");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng.", Toast.LENGTH_LONG).show();
            finish(); // Đóng Activity nếu không có ID
            return;
        }

        loadUserData();

        btnSaveChanges.setOnClickListener(v -> saveRoleChanges());
    }

    private void loadUserData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserEntity user = documentSnapshot.toObject(UserEntity.class);
                        if (user != null) {
                            // Điền thông tin vào các TextView
                            tvFullName.setText(user.getName()); // Sửa thành getFullName()
                            tvEmail.setText(user.getEmail());
                            tvPhoneNumber.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "Chưa cập nhật");

                            // --- PHẦN SỬA LỖI QUAN TRỌNG NHẤT ---
                            // Xử lý ngày tạo dạng String thay vì long
                            String createdAtString = user.getCreatedAt();
                            if (createdAtString != null && !createdAtString.isEmpty()) {
                                try {
                                    // 1. Định dạng của chuỗi gốc trong Firestore
                                    // Chú ý: .SSSSSS là cho microsecond, nếu chỉ có 3 số thì dùng .SSS
                                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
                                    Date date = inputFormat.parse(createdAtString);

                                    // 2. Định dạng bạn muốn hiển thị ra màn hình
                                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                    tvCreationDate.setText(outputFormat.format(date));

                                } catch (ParseException e) {
                                    Log.e("UserEditActivity", "Lỗi parse ngày tháng: ", e);
                                    tvCreationDate.setText("Ngày không hợp lệ");
                                }
                            } else {
                                tvCreationDate.setText("Không rõ");
                            }

                            // Chọn đúng vai trò trong RadioGroup
                            String role = user.getRole() != null ? user.getRole().toLowerCase() : "user";
                            switch (role) {
                                case "admin":
                                    rbAdmin.setChecked(true);
                                    break;
                                case "employee":
                                    rbEmployee.setChecked(true);
                                    break;
                                case "user":
                                default:
                                    rbCustomer.setChecked(true);
                                    break;
                            }
                        }
                    } else {
                        Toast.makeText(this, "Người dùng không tồn tại.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void saveRoleChanges() {
        // Xác định vai trò mới được chọn
        int selectedId = rgRole.getCheckedRadioButtonId();
        String newRole;
        if (selectedId == R.id.rb_role_admin) {
            newRole = "admin";
        } else if (selectedId == R.id.rb_role_employee) {
            newRole = "employee";
        } else {
            newRole = "user";
        }

        // Chỉ cập nhật trường "role" trong Firestore
        db.collection("users").document(userId)
                .update("role", newRole)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật vai trò thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Đóng Activity sau khi cập nhật
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
