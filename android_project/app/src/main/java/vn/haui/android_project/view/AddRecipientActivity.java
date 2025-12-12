package vn.haui.android_project.view;

import android.content.Intent;
import android.content.res.ColorStateList;
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

import androidx.activity.EdgeToEdge;
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

public class AddRecipientActivity extends AppCompatActivity {
    private EditText et_recipient_name, et_phone_number, et_address, et_country, et_zip_code;
    private Button btn_save;
    private ImageButton btnBack;
    private ChipGroup chipGroupLocationType;
    private Chip chipHome, chipWork, chipOther;
    private SwitchMaterial switchDefaultAddress;
    private FirebaseLocationManager firebaseLocationManager;

    private TextView chooseMap;

    private double latitude, longitude;
    private String address;

    private WebView webViewMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_recipient);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_add_recipient), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseLocationManager = new FirebaseLocationManager();
        mapping();
        setupChipStyling(chipHome);
        setupChipStyling(chipWork);
        setupChipStyling(chipOther);

        handleIntentData();

        setupChipGroupListener();
        setupWebView(); // Cấu hình WebView

        chooseMap.setOnClickListener(v -> {
            // Sửa lại: Chuyển sang MapLocationActivity thay vì SelectLocationActivity
            Intent intent = new Intent(AddRecipientActivity.this, MapLocationActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("address", et_address.getText().toString());
            // Thêm một flag để MapLocationActivity biết cần trả kết quả về đâu
            intent.putExtra("activityView", "addNew");
            startActivity(intent);
        });
        btnBack.setOnClickListener(v -> finish());
        btn_save.setOnClickListener(v -> saveRecipient());
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            // Mặc định là Hà Nội nếu không có tọa độ
            latitude = intent.getDoubleExtra("latitude", 21.0285);
            longitude = intent.getDoubleExtra("longitude", 105.8542);
            address = intent.getStringExtra("address");
            if (address != null) {
                et_address.setText(address);
            }
        }
    }

    private void mapping() {
        webViewMap = findViewById(R.id.webview_map_add);
        chooseMap = findViewById(R.id.chooseMap);
        et_recipient_name = findViewById(R.id.et_recipient_name);
        et_phone_number = findViewById(R.id.et_phone_number);
        et_address = findViewById(R.id.et_address);
        et_country = findViewById(R.id.et_country);
        et_zip_code = findViewById(R.id.et_zip_code);
        btnBack = findViewById(R.id.btn_back);
        btn_save = findViewById(R.id.btn_save);
        chipGroupLocationType = findViewById(R.id.chip_group_location_type);
        chipHome = findViewById(R.id.chip_home);
        chipWork = findViewById(R.id.chip_work);
        chipOther = findViewById(R.id.chip_other);
        switchDefaultAddress = findViewById(R.id.switch_default_address);
    }

    private void setupWebView() {
        // Cài đặt cơ bản
        webViewMap.getSettings().setJavaScriptEnabled(true);
        webViewMap.getSettings().setDomStorageEnabled(true);

        // --- THÊM CÁC CÀI ĐẶT QUAN TRỌNG ---
        webViewMap.getSettings().setAllowFileAccess(true);
        webViewMap.getSettings().setAllowContentAccess(true);

        // Vô hiệu hóa cuộn và tương tác chạm
        webViewMap.setVerticalScrollBarEnabled(false);
        webViewMap.setHorizontalScrollBarEnabled(false);
        webViewMap.setOnTouchListener((v, event) -> true); // Vô hiệu hóa hoàn toàn touch

        // Không cần cầu nối Javascript vì bản đồ này chỉ để hiển thị
        // webViewMap.addJavascriptInterface(new WebAppInterfaceAdd(), "Android");

        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Căn giữa bản đồ sau khi trang đã tải xong
                centerMapAtLocation(latitude, longitude, 15); // Zoom xa hơn một chút
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.e("WebViewError-Add", "Error: " + error.getDescription() + " for URL: " + request.getUrl().toString());
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


    // --- CÁC PHƯƠNG THỨC KHÁC GIỮ NGUYÊN ---

    private void setupChipStyling(Chip chip) {
        if (chip == null) return;
        int COLOR_RED = Color.parseColor("#EB4D57");
        int COLOR_WHITE = Color.WHITE;
        int COLOR_DEFAULT_GRAY = Color.parseColor("#F0F0F0");
        int COLOR_BLACK = Color.BLACK;
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] backgroundColors = new int[]{
                COLOR_RED,
                COLOR_DEFAULT_GRAY
        };
        ColorStateList backgroundCsl = new ColorStateList(states, backgroundColors);
        int[] textIconColors = new int[]{
                COLOR_WHITE,
                COLOR_BLACK
        };
        ColorStateList textIconCsl = new ColorStateList(states, textIconColors);
        chip.setChipBackgroundColor(backgroundCsl);
        chip.setTextColor(textIconCsl);
        chip.setChipIconTint(textIconCsl);
        chip.setChipStrokeWidth(0f);
        chip.setCheckable(true);
    }

    private void setupChipGroupListener() {
        if (chipGroupLocationType == null) return;
        chipGroupLocationType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Vui lòng chọn loại địa điểm.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveRecipient() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String recipientName = et_recipient_name.getText().toString().trim();
        String phoneNumber = et_phone_number.getText().toString().trim();
        String currentAddress = et_address.getText().toString().trim();
        String country = et_country.getText().toString().trim();
        String zipCode = et_zip_code.getText().toString().trim();
        String locationType = getSelectedLocationType();
        boolean isDefault = switchDefaultAddress != null && switchDefaultAddress.isChecked();

        if (recipientName.isEmpty() || currentAddress.isEmpty() || locationType.equals("N/A")) {
            Toast.makeText(this, "Vui lòng điền đầy đủ Tên, Địa chỉ và chọn Loại địa điểm.", Toast.LENGTH_LONG).show();
            return;
        }
        if (phoneNumber.isEmpty()) {
            phoneNumber = authUser.getPhoneNumber() != null ? authUser.getPhoneNumber() : "";
        }
        if (!isDefault) {
            performSave(authUser.getUid(), buildLocationEntity(false, recipientName, phoneNumber, currentAddress, locationType, country, zipCode));
            return;
        }
        String finalPhoneNumber = phoneNumber;
        firebaseLocationManager.hasDefaultLocation(authUser.getUid(),null, (success, hasDefault) -> {
            if (success) {
                if (hasDefault) {
                    Toast.makeText(this, "Đã có 1 địa chỉ mặc định khác. Vui lòng tắt tùy chọn này để lưu.", Toast.LENGTH_LONG).show();
                } else {
                    performSave(authUser.getUid(), buildLocationEntity(true, recipientName, finalPhoneNumber, currentAddress, locationType, country, zipCode));
                }
            } else {
                Toast.makeText(this, "Lỗi kiểm tra trạng thái mặc định. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            }
        });
    }
    private UserLocationEntity buildLocationEntity(boolean isDefault, String recipientName, String phoneNumber, String address, String locationType, String country, String zipCode) {
        return new UserLocationEntity(
                String.valueOf(System.currentTimeMillis()),
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

    private void performSave(String uid, UserLocationEntity location) {
        firebaseLocationManager.appendLocation(
                uid,
                location,
                (success, msg) -> {
                    if (success) {
                        Toast.makeText(AddRecipientActivity.this, "Thêm địa chỉ thành công!", Toast.LENGTH_SHORT).show();
                        // Chuyển về màn hình danh sách địa chỉ và kết thúc activity này
                        Intent intent = new Intent(this, ChooseRecipientActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(AddRecipientActivity.this, "Lỗi thêm địa chỉ: " + msg, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private String getSelectedLocationType() {
        if (chipGroupLocationType == null) return "N/A";
        int checkedChipId = chipGroupLocationType.getCheckedChipId();
        if (checkedChipId == R.id.chip_home) {
            return "Home";
        } else if (checkedChipId == R.id.chip_work) {
            return "Work";
        } else if (checkedChipId == R.id.chip_other) {
            return "Other";
        } else {
            return "N/A";
        }
    }
    // Lớp WebAppInterfaceAdd không còn cần thiết vì bản đồ này chỉ hiển thị
}

