package vn.haui.android_project.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.RecipientAdapter;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.services.FirebaseLocationManager;

public class ChooseRecipientActivity extends AppCompatActivity {

    private RecyclerView recyclerRecipients;
    private Button btnAddRecipient;
    private FirebaseLocationManager firebaseLocationManager;

    // Khai báo Adapter là biến thành viên để dễ dàng tham chiếu và cập nhật
    private RecipientAdapter recipientAdapter;

    // Khởi tạo danh sách trống ban đầu (để tránh lỗi NullPointerException)
    private List<UserLocationEntity> recipientList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose_recipient);
        // Cập nhật ID layout cho setOnApplyWindowInsetsListener
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_choose_recipient), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        firebaseLocationManager = new FirebaseLocationManager();
        // --- 1. Ánh xạ Views ---
        recyclerRecipients = findViewById(R.id.recycler_recipients);
        btnAddRecipient = findViewById(R.id.btn_add_recipient);
        // --- 2. Khởi tạo RecyclerView và Adapter trống ---
        // Khởi tạo Adapter với list trống
        recipientAdapter = new RecipientAdapter(recipientList,this);
        recyclerRecipients.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecipients.setAdapter(recipientAdapter);
        // --- 3. Tải dữ liệu từ Firebase ---
        loadRecipients();
        // --- 4. Xử lý Sự kiện Nút "Add Recipient" ---
        btnAddRecipient.setOnClickListener(v -> {
            // TODO: Chuyển sang màn hình thêm địa chỉ mới
            // Intent intent = new Intent(this, AddNewRecipientActivity.class);
            // startActivity(intent);
        });
    }

    private void loadRecipients() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        firebaseLocationManager.getSortedLocationsByUid(authUser.getUid(), (success, sortedList) -> {
            if (success) {
                // Xóa dữ liệu cũ và thêm dữ liệu mới đã sắp xếp
                recipientList.clear();
                if (sortedList != null) {
                    recipientList.addAll(sortedList);
                }
                recipientAdapter.notifyDataSetChanged();
                if (recipientList.isEmpty()) {
                    Toast.makeText(this, "Không tìm thấy địa chỉ nào.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Lỗi khi tải danh sách địa chỉ.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}