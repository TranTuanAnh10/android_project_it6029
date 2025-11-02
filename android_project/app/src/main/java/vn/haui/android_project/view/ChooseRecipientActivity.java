package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
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
import vn.haui.android_project.services.LocationService;

// BẮT BUỘC: Implement Interface để nhận sự kiện click từ Adapter
public class ChooseRecipientActivity extends AppCompatActivity
        implements RecipientAdapter.OnLocationActionListener {

    private RecyclerView recyclerRecipients;
    private Button btnAddRecipient;
    private FirebaseLocationManager firebaseLocationManager;
    private RecipientAdapter recipientAdapter;
    private List<UserLocationEntity> recipientList = new ArrayList<>();

    // Khai báo Request Code để nhận biết Activity nào trả về
    private static final int EDIT_RECIPIENT_REQUEST_CODE = 1;
    private static final int ADD_RECIPIENT_REQUEST_CODE = 2;
    private static final int DELETE_RESULT_CODE = 100; // Mã tùy chỉnh cho thao tác xóa
    private double latitude, longitude;
    private String address;
    private LocationService locationService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose_recipient);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_choose_recipient), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Đặt padding cho toàn bộ activity
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        locationService = new LocationService(this);
        firebaseLocationManager = new FirebaseLocationManager();
        recyclerRecipients = findViewById(R.id.recycler_recipients);
        btnAddRecipient = findViewById(R.id.btn_add_recipient);
        recipientAdapter = new RecipientAdapter(recipientList, this, this);
        recyclerRecipients.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecipients.setAdapter(recipientAdapter);
        btnAddRecipient.setOnClickListener(v -> {
            getLocation();
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadRecipients();
    }
    private void startSelectLocationActivity() {
        Intent intent = new Intent(this, SelectLocationActivity.class);
        intent.putExtra("activityView", "addNew");
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        intent.putExtra("address", address);
        startActivityForResult(intent, ADD_RECIPIENT_REQUEST_CODE);
    }

    private void getLocation() {
        locationService.getCurrentLocation(new LocationService.LocationCallbackListener() {
            @Override
            public void onLocationResult(double la, double log, String add) {
                latitude = la;
                longitude = log;
                address = add;
                startSelectLocationActivity();
            }
            @Override
            public void onLocationError(String errorMessage) {
                Toast.makeText(ChooseRecipientActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadRecipients() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        firebaseLocationManager.getSortedLocationsByUid(authUser.getUid(), (success, sortedList) -> {
            if (success) {
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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_RECIPIENT_REQUEST_CODE || requestCode == ADD_RECIPIENT_REQUEST_CODE) {
            if (resultCode == RESULT_OK || resultCode == DELETE_RESULT_CODE) {
                String message;
                if (requestCode == ADD_RECIPIENT_REQUEST_CODE && resultCode == RESULT_OK) {
                    message = "Địa chỉ mới đã được thêm thành công.";
                } else if (resultCode == RESULT_OK) {
                    message = "Địa chỉ đã được cập nhật thành công.";
                } else {
                    message = "Địa chỉ đã được xóa.";
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onEditClick(UserLocationEntity location) {
        Intent intent = new Intent(this, EditRecipientActivity.class);
        intent.putExtra("location_id", location.getId());
        intent.putExtra("recipientName", location.getRecipientName());
        intent.putExtra("phoneNumber", location.getPhoneNumber());
        intent.putExtra("address", location.getAddress());
        intent.putExtra("locationType", location.getLocationType());
        intent.putExtra("defaultLocation", location.isDefaultLocation());
        intent.putExtra("country", location.getCountry());
        intent.putExtra("zipCode", location.getZipCode());

        intent.putExtra("latitude", location.getLatitude());
        intent.putExtra("longitude", location.getLongitude());
        startActivityForResult(intent, EDIT_RECIPIENT_REQUEST_CODE);
    }
    @Override
    public void onSelectLocation(UserLocationEntity location) {
        // Xử lý logic chọn địa chỉ, thường là kết thúc Activity và trả về kết quả
        Toast.makeText(this, "Đã chọn địa chỉ: " + location.getLocationType(), Toast.LENGTH_SHORT).show();

        // Ví dụ hoàn chỉnh để trả về kết quả cho màn hình trước đó:
        // Intent resultIntent = new Intent();
        // resultIntent.putExtra("selected_location_id", location.getId());
        // setResult(RESULT_OK, resultIntent);
        // finish();
    }
}