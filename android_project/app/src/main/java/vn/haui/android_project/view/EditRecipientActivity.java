package vn.haui.android_project.view;

import android.content.Intent;import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Locale;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.services.FirebaseLocationManager;

public class EditRecipientActivity extends AppCompatActivity {
    private EditText et_recipient_name, et_phone_number, et_address, et_country, et_zip_code;
    private Button btn_save, btn_delete;
    private ImageButton btnBack;
    private ChipGroup chipGroupLocationType;
    private Chip chipHome, chipWork, chipOther;
    private SwitchMaterial switchDefaultAddress;
    private FirebaseLocationManager firebaseLocationManager;

    private WebView webViewMap;
    private TextView chooseMapEdit;

    private String locationId;
    private double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipient);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_edit_recipient), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mapping();
        firebaseLocationManager = new FirebaseLocationManager();

        handleIntentData(); // Lấy và hiển thị dữ liệu từ Intent

        setupChipStyling(chipHome);
        setupChipStyling(chipWork);
        setupChipStyling(chipOther);
        setupChipGroupListener();
        setupWebView(); // Cấu hình WebView

        btnBack.setOnClickListener(v -> finish());
        btn_save.setOnClickListener(v -> saveRecipient());
        btn_delete.setOnClickListener(v -> deleteRecipient());

        chooseMapEdit.setOnClickListener(v -> {
            // Tạo một đối tượng UserLocationEntity với dữ liệu hiện tại trên form
            UserLocationEntity currentLocation = buildLocationEntityFromForm();

            Intent intent = new Intent(EditRecipientActivity.this, MapLocationActivity.class);
            // Gửi toàn bộ đối tượng để MapLocationActivity có thể điền lại form khi quay về
            intent.putExtra("locationToSave", currentLocation);
            intent.putExtra("activityView", "updateChoose");
            startActivity(intent);
        });
    }

    private void mapping() {
        et_address = findViewById(R.id.et_address);
        et_phone_number = findViewById(R.id.et_phone_number);
        et_recipient_name = findViewById(R.id.et_recipient_name);
        et_country = findViewById(R.id.et_country);
        et_zip_code = findViewById(R.id.et_zip_code);
        btn_save = findViewById(R.id.btn_save);
        btn_delete = findViewById(R.id.btn_delete);
        chipGroupLocationType = findViewById(R.id.chip_group_location_type);
        btnBack = findViewById(R.id.btn_back);
        switchDefaultAddress = findViewById(R.id.switch_default_address);
        chipHome = findViewById(R.id.chip_home);
        chipWork = findViewById(R.id.chip_work);
        chipOther = findViewById(R.id.chip_other);
        webViewMap = findViewById(R.id.webview_map_edit);
        chooseMapEdit= findViewById(R.id.chooseMapEdit);
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, "Không có dữ liệu để chỉnh sửa.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        locationId = intent.getStringExtra("location_id");
        String recipientName = intent.getStringExtra("recipientName");
        String phoneNumber = intent.getStringExtra("phoneNumber");
        String address = intent.getStringExtra("address");
        String country = intent.getStringExtra("country");
        String zipCode = intent.getStringExtra("zipCode");
        String locationType = intent.getStringExtra("locationType");
        boolean isDefault = intent.getBooleanExtra("defaultLocation", false);
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);

        et_recipient_name.setText(recipientName);
        et_phone_number.setText(phoneNumber);
        et_address.setText(address);
        et_country.setText(country);
        et_zip_code.setText(zipCode);
        switchDefaultAddress.setChecked(isDefault);

        if ("Home".equalsIgnoreCase(locationType)) {
            chipGroupLocationType.check(R.id.chip_home);
        } else if ("Work".equalsIgnoreCase(locationType)) {
            chipGroupLocationType.check(R.id.chip_work);
        } else {
            chipGroupLocationType.check(R.id.chip_other);
        }
    }

    private void setupWebView() {
        webViewMap.getSettings().setJavaScriptEnabled(true);
        webViewMap.getSettings().setDomStorageEnabled(true);

        // --- THÊM CÁC CÀI ĐẶT QUAN TRỌNG ---
        webViewMap.getSettings().setAllowFileAccess(true);
        webViewMap.getSettings().setAllowContentAccess(true);

        // Vô hiệu hóa cuộn và tương tác chạm
        webViewMap.setVerticalScrollBarEnabled(false);
        webViewMap.setHorizontalScrollBarEnabled(false);
        webViewMap.setOnTouchListener((v, event) -> true); // Vô hiệu hóa hoàn toàn touch

        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Căn giữa bản đồ sau khi trang đã tải xong
                centerMapAtLocation(latitude, longitude, 15);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.e("WebViewError-Edit", "Error: " + error.getDescription() + " for URL: " + request.getUrl().toString());
                }
            }
        });
        webViewMap.loadUrl("file:///android_asset/map_selector.html");
    }

    private void centerMapAtLocation(double lat, double lng, int zoom) {
        // Dùng Locale.US để đảm bảo dấu thập phân là "."
        final String jsCode = String.format(Locale.US, "javascript:centerMapAtLocation(%.6f, %.6f, %d);", lat, lng, zoom);
        webViewMap.evaluateJavascript(jsCode, null);
    }

    // --- CÁC PHƯƠNG THỨC KHÁC ---

    private UserLocationEntity buildLocationEntityFromForm() {
        String recipientName = et_recipient_name.getText().toString().trim();
        String phoneNumber = et_phone_number.getText().toString().trim();
        String address = et_address.getText().toString().trim();
        String country = et_country.getText().toString().trim();
        String zipCode = et_zip_code.getText().toString().trim();
        String locationType = getSelectedLocationType();
        boolean isDefault = switchDefaultAddress.isChecked();

        return new UserLocationEntity(
                locationId,
                recipientName,
                latitude, longitude,
                address,
                phoneNumber,
                isDefault,
                locationType,
                country,
                zipCode
        );
    }

    private void saveRecipient() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        UserLocationEntity locationToSave = buildLocationEntityFromForm();

        if (locationToSave.getRecipientName().isEmpty() || locationToSave.getAddress().isEmpty() || "N/A".equals(locationToSave.getLocationType())) {
            Toast.makeText(this, "Vui lòng điền đầy đủ Tên, Địa chỉ và chọn Loại địa điểm.", Toast.LENGTH_LONG).show();
            return;
        }

        if (locationToSave.getPhoneNumber().isEmpty()) {
            locationToSave.setPhoneNumber(authUser.getPhoneNumber() != null ? authUser.getPhoneNumber() : "");
        }

        if (!locationToSave.isDefaultLocation()) {
            performUpdate(authUser.getUid(), locationToSave);
            return;
        }

        firebaseLocationManager.hasDefaultLocation(authUser.getUid(), locationId, (success, hasDefault) -> {
            if (success) {
                if (hasDefault) {
                    Toast.makeText(this, "Địa chỉ mặc định đã tồn tại. Vui lòng tắt tùy chọn này hoặc chỉnh sửa địa chỉ mặc định cũ.", Toast.LENGTH_LONG).show();
                } else {
                    performUpdate(authUser.getUid(), locationToSave);
                }
            } else {
                Toast.makeText(this, "Lỗi kiểm tra trạng thái mặc định. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performUpdate(String uid, UserLocationEntity location) {
        firebaseLocationManager.updateLocation(uid, location, (isSuccess, message) -> {
            if (isSuccess) {
                Toast.makeText(EditRecipientActivity.this, "Cập nhật địa chỉ thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, ChooseRecipientActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(EditRecipientActivity.this, "Lỗi cập nhật: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteRecipient() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        if (locationId == null || locationId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ID địa chỉ để xóa.", Toast.LENGTH_SHORT).show();
            return;
        }
        firebaseLocationManager.deleteLocationById(authUser.getUid(), locationId, (isSuccess, message) -> {
            if (isSuccess) {
                Toast.makeText(EditRecipientActivity.this, "Đã xóa địa chỉ.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, ChooseRecipientActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(EditRecipientActivity.this, "Lỗi xóa: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Các phương thức tiện ích không đổi
    private String getSelectedLocationType() {
        int checkedChipId = chipGroupLocationType.getCheckedChipId();
        if (checkedChipId == R.id.chip_home) {
            return "Home";
        } else if (checkedChipId == R.id.chip_work) {
            return "Work";
        } else if (checkedChipId == R.id.chip_other) {
            return "Other";
        }
        return "N/A";
    }

    private void setupChipStyling(Chip chip) {
        if (chip == null) return;
        int COLOR_RED = Color.parseColor("#EB4D57");
        int COLOR_WHITE = Color.WHITE;
        int COLOR_DEFAULT_GRAY = Color.parseColor("#F0F0F0");
        int COLOR_BLACK = Color.BLACK;
        int[][] states = new int[][]{ new int[]{android.R.attr.state_checked}, new int[]{} };
        int[] backgroundColors = new int[]{ COLOR_RED, COLOR_DEFAULT_GRAY };
        ColorStateList backgroundCsl = new ColorStateList(states, backgroundColors);
        int[] textIconColors = new int[]{ COLOR_WHITE, COLOR_BLACK };
        ColorStateList textIconCsl = new ColorStateList(states, textIconColors);
        chip.setChipBackgroundColor(backgroundCsl);
        chip.setTextColor(textIconCsl);
        chip.setChipIconTint(textIconCsl);
        chip.setChipStrokeWidth(0f);
        chip.setCheckable(true);
    }

    private void setupChipGroupListener() {
        chipGroupLocationType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            // Không cần làm gì ở đây vì đã xử lý lúc lưu
        });
    }
}
