package vn.haui.android_project.view;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button; // Sửa: Sử dụng Button cho nút Save
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Sử dụng Toolbar cho nút back
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.haui.android_project.R;
import vn.haui.android_project.service.FirebaseUserManager;

import java.util.HashMap;
import java.util.Map;

public class EditProfileScreenActivity extends AppCompatActivity {
    private TextView userNameDisplay;
    private EditText edtName, edtPhoneNumber, edtEmail; // Thống nhất cách đặt tên
    private FloatingActionButton fabEditAvatar;
    private ImageView imgUser;
    private Button btnSave;
    private ImageButton toolbar;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private FirebaseUserManager userManager;
    private Uri selectedImageUri = null; // Biến lưu ảnh mới được chọn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_profile_screen);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.edit_profile_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mapping();
        init();
        setupListeners();
    }

    private void mapping() {
        toolbar = findViewById(R.id.btn_back);
        userNameDisplay = findViewById(R.id.tv_user_name_display);
        edtName = findViewById(R.id.et_name);
        edtPhoneNumber = findViewById(R.id.et_phone);
        edtEmail = findViewById(R.id.et_email);
        imgUser = findViewById(R.id.iv_avatar);
        fabEditAvatar = findViewById(R.id.fab_edit_avatar);
        btnSave = findViewById(R.id.btn_save); // Ánh xạ nút Save
    }

    private void init() {
        userManager = new FirebaseUserManager();
        loadUserProfile();
        setupImagePicker();
    }

    private void loadUserProfile() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish(); // Đóng activity nếu chưa đăng nhập
            return;
        }

        userManager.getUserByUid(authUser.getUid(), userData -> {
            // Lấy dữ liệu từ Firestore hoặc từ Auth nếu Firestore không có
            String name = (String) userData.getOrDefault("name", authUser.getDisplayName());
            String email = (String) userData.getOrDefault("email", authUser.getEmail());
            String avatarUrl = (String) userData.getOrDefault("avatarUrl",
                    (authUser.getPhotoUrl() != null) ? authUser.getPhotoUrl().toString() : null);
            String phone = (String) userData.getOrDefault("phoneNumber", "");

            // Gán dữ liệu lên giao diện
            userNameDisplay.setText(name);
            edtName.setText(name);
            edtPhoneNumber.setText(phone);
            edtEmail.setText(email);

            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_user) // Icon placeholder
                        .circleCrop() // Bo tròn ảnh
                        .into(imgUser);
            } else {
                imgUser.setImageResource(R.drawable.ic_user);
            }
        }, error -> {
            Toast.makeText(this, "Không tìm thấy thông tin tài khoản", Toast.LENGTH_SHORT).show();
        });
    }
    private void setupImagePicker() {
        // Khởi tạo launcher để nhận kết quả chọn ảnh
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        // Hiển thị ảnh mới được chọn
                        Glide.with(this)
                                .load(selectedImageUri)
                                .circleCrop()
                                .into(imgUser);
                    }
                });
    }
    private void setupListeners() {
        // Sử dụng Toolbar để xử lý nút back
        toolbar.setOnClickListener(v -> onBackPressed());
        fabEditAvatar.setOnClickListener(v -> {
            // Mở thư viện ảnh
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
        btnSave.setOnClickListener(v -> {
            saveUserProfile();
        });
    }

    private void saveUserProfile() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) return;

        String newName = edtName.getText().toString().trim();
        String newPhone = edtPhoneNumber.getText().toString().trim();

        btnSave.setEnabled(false);
        Toast.makeText(this, "Đang cập nhật...", Toast.LENGTH_SHORT).show();

        if (selectedImageUri != null) {
            // 1. Nếu có ảnh mới, upload ảnh trước
            // SỬA TÊN HÀM: updateAvatar -> uploadAvatar
            userManager.uploadAvatar(authUser.getUid(), selectedImageUri,
                    (isSuccess, resultUrl) -> {
                        if (isSuccess) {
                            // 2. Upload ảnh thành công, cập nhật thông tin còn lại
                            updateUserInfo(authUser.getUid(), newName, newPhone, resultUrl);
                        } else {
                            // resultUrl lúc này là thông báo lỗi
                            Toast.makeText(this, "Tải ảnh lên thất bại: " + resultUrl, Toast.LENGTH_LONG).show();
                            btnSave.setEnabled(true);
                        }
                    });
        } else {
            // 1. Nếu không có ảnh mới, chỉ cập nhật thông tin
            updateUserInfo(authUser.getUid(), newName, newPhone, null);
        }
    }


    private void updateUserInfo(String uid, String name, String phone, String avatarUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phoneNumber", phone);
        if (avatarUrl != null) {
            updates.put("avatarUrl", avatarUrl);
        }

        // SỬA TÊN HÀM: updateUserInfo -> updateUser
        userManager.updateUser(uid, updates,
                aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    finish();
                },
                e -> {
                    Toast.makeText(this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
    }
}
