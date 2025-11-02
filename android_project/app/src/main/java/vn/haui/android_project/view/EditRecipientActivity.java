package vn.haui.android_project.view;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.webkit.JavascriptInterface;
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

import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.services.FirebaseLocationManager;
import vn.haui.android_project.services.LocationService;

public class EditRecipientActivity extends AppCompatActivity {
    private String locationId, address, phoneNumber, defaultLocation, locationType, recipientName, country, zipCode;
    private EditText et_recipient_name, et_phone_number, et_address, et_country, et_zip_code;
    private Button btn_save, btn_delete;
    private ImageButton btnBack;
    private ChipGroup chipGroupLocationType;
    private Chip chipHome, chipWork, chipOther;
    private SwitchMaterial switchDefaultAddress;
    private static final int DELETE_RESULT_CODE = 100; // Đảm bảo mã này khớp với ChooseRecipientActivity
    private FirebaseLocationManager firebaseLocationManager;

    private WebView webViewMap;
    private TextView chooseMapEdit;
    private LocationService locationService;
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
        setupChipStyling(chipHome);
        setupChipStyling(chipWork);
        setupChipStyling(chipOther);
        setupChipGroupListener();
        btnBack.setOnClickListener(v -> finish());
        Intent intent = getIntent();
        if (intent != null) {
            locationId = intent.getStringExtra("location_id");
            address = intent.getStringExtra("address");
            phoneNumber = intent.getStringExtra("phoneNumber");
            defaultLocation = String.valueOf(intent.getBooleanExtra("defaultLocation", false));
            locationType = intent.getStringExtra("locationType");
            recipientName = intent.getStringExtra("recipientName");
            country = intent.getStringExtra("country");
            zipCode = intent.getStringExtra("zipCode");
            latitude = intent.getDoubleExtra("latitude", 0.0);
            longitude = intent.getDoubleExtra("longitude", 0.0);
            setupWebView();


            et_recipient_name.setText(recipientName);
            et_phone_number.setText(phoneNumber);
            et_address.setText(address);
            boolean isDefault = "true".equalsIgnoreCase(defaultLocation);
            switchDefaultAddress.setChecked(isDefault);
            et_country.setText(country);
            et_zip_code.setText(zipCode);
        }

        // --- 5. Set Chip Mặc định theo dữ liệu ---
        int chipIdToSelect = -1;
        if ("Home".equalsIgnoreCase(locationType)) {
            chipIdToSelect = R.id.chip_home;
        } else if ("Work".equalsIgnoreCase(locationType)) {
            chipIdToSelect = R.id.chip_work;
        } else if ("Other".equalsIgnoreCase(locationType)) {
            chipIdToSelect = R.id.chip_other;
        }
        if (chipIdToSelect != -1) {
            chipGroupLocationType.check(chipIdToSelect);
        }

        btn_save.setOnClickListener(v -> saveRecipient());
        btn_delete.setOnClickListener(v -> deleteRecipient());
        chooseMapEdit.setOnClickListener(v -> {
            String updatedRecipientName = et_recipient_name.getText().toString().trim();
            String updatedPhoneNumber = et_phone_number.getText().toString().trim();
            String updatedAddress = et_address.getText().toString().trim();
            String updatedCountry = et_country.getText().toString().trim();
            String updatedZipCode = et_zip_code.getText().toString().trim();
            String updatedLocationType = getSelectedLocationType();
            boolean isDefault = switchDefaultAddress != null && switchDefaultAddress.isChecked();
            UserLocationEntity locationToSave = new UserLocationEntity(
                    locationId,
                    updatedRecipientName,
                    latitude, longitude,
                    updatedAddress,
                    updatedPhoneNumber,
                    isDefault,
                    updatedLocationType,
                    updatedCountry,
                    updatedZipCode
            );
            Intent intent1 = new Intent(EditRecipientActivity.this, SelectLocationActivity.class);
            intent1.putExtra("locationToSave", locationToSave);
            intent1.putExtra("activityView", "updateChoose");
            startActivity(intent1);
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
        btnBack = findViewById(R.id.btn_back); // Ánh xạ nút Back
        switchDefaultAddress = findViewById(R.id.switch_default_address);
        // Ánh xạ 3 CHIP BỊ THIẾU
        chipHome = findViewById(R.id.chip_home);
        chipWork = findViewById(R.id.chip_work);
        chipOther = findViewById(R.id.chip_other);
        webViewMap = findViewById(R.id.webview_map_edit);
        locationService = new LocationService(this);
        chooseMapEdit= findViewById(R.id.chooseMapEdit);
    }

    private void setupWebView() {
        // Cài đặt cơ bản (Giữ nguyên)
        webViewMap.getSettings().setJavaScriptEnabled(true);
        webViewMap.getSettings().setDomStorageEnabled(true);
        webViewMap.addJavascriptInterface(new WebAppInterfaceEdit(), "Android");
        webViewMap.setVerticalScrollBarEnabled(false);
        webViewMap.setHorizontalScrollBarEnabled(false);
        webViewMap.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webViewMap.setOnTouchListener((v, event) -> (event.getAction() == MotionEvent.ACTION_MOVE));
        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Đảm bảo hàm centerMapAtLocation đã tồn tại
                centerMapAtLocation(latitude, longitude, 14);
            }
        });
        webViewMap.loadUrl("file:///android_asset/map_selector.html");
    }
    private void centerMapAtLocation(double lat, double lng, int zoom) {
        final String jsCode = String.format("javascript:centerMapAtLocation(%.6f, %.6f, %d);", lat, lng, zoom);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webViewMap.evaluateJavascript(jsCode, null);
        } else {
            webViewMap.loadUrl(jsCode);
        }
    }
    public class WebAppInterfaceEdit {
        @JavascriptInterface
        public void onLocationChanged(final double lat, final double lng) {
            runOnUiThread(() -> {
                latitude = lat;
                longitude = lng;
            });
        }
    }

    private void setupChipStyling(Chip chip) {
        // Kiểm tra null để đảm bảo không lỗi nếu chưa ánh xạ
        if (chip == null) return;

        // 1. ĐỊNH NGHĨA MÀU SẮC
        int COLOR_RED = Color.parseColor("#EB4D57");
        int COLOR_WHITE = Color.WHITE;
        int COLOR_DEFAULT_GRAY = Color.parseColor("#F0F0F0");
        int COLOR_BLACK = Color.BLACK;

        // 2. ĐỊNH NGHĨA TRẠNG THÁI
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };

        // 3. TẠO COLOR STATE LIST CHO NỀN
        int[] backgroundColors = new int[]{
                COLOR_RED,
                COLOR_DEFAULT_GRAY
        };
        ColorStateList backgroundCsl = new ColorStateList(states, backgroundColors);

        // 4. TẠO COLOR STATE LIST CHO CHỮ & ICON
        int[] textIconColors = new int[]{
                COLOR_WHITE,
                COLOR_BLACK
        };
        ColorStateList textIconCsl = new ColorStateList(states, textIconColors);

        // 5. ÁP DỤNG CÁC CSL VÀO CHIP
        chip.setChipBackgroundColor(backgroundCsl);
        chip.setTextColor(textIconCsl);
        chip.setChipIconTint(textIconCsl);
        chip.setChipStrokeWidth(0f);
        chip.setCheckable(true);
    }

    private void setupChipGroupListener() {
        chipGroupLocationType.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup chipGroup, @NonNull List<Integer> checkedIds) {
                if (!checkedIds.isEmpty()) {
                    int checkedChipId = checkedIds.get(0);
                    // Cập nhật biến locationType khi người dùng chọn Chip mới
                    if (checkedChipId == R.id.chip_home) {
                        locationType = "Home";
                    } else if (checkedChipId == R.id.chip_work) {
                        locationType = "Work";
                    } else if (checkedChipId == R.id.chip_other) {
                        locationType = "Other";
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Chưa chọn loại địa điểm.", Toast.LENGTH_SHORT).show();
                    locationType = null;
                }
            }
        });
    }

    private String getSelectedLocationType() {
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


    private void saveRecipient() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String updatedRecipientName = et_recipient_name.getText().toString().trim();
        String updatedPhoneNumber = et_phone_number.getText().toString().trim();
        String updatedAddress = et_address.getText().toString().trim();
        String updatedCountry = et_country.getText().toString().trim();
        String updatedZipCode = et_zip_code.getText().toString().trim();
        String updatedLocationType = getSelectedLocationType();
        boolean isDefault = switchDefaultAddress != null && switchDefaultAddress.isChecked();
        if (updatedRecipientName.isEmpty() || updatedAddress.isEmpty() || updatedLocationType.equals("N/A")) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin và chọn loại địa điểm.", Toast.LENGTH_LONG).show();
            return;
        }
        if (updatedPhoneNumber.isEmpty()) {
            updatedPhoneNumber = authUser.getPhoneNumber() != null ? authUser.getPhoneNumber() : "";
        }
        UserLocationEntity locationToSave = new UserLocationEntity(
                locationId,
                updatedRecipientName,
                latitude, longitude,
                updatedAddress,
                updatedPhoneNumber,
                isDefault,
                updatedLocationType,
                updatedCountry,
                updatedZipCode
        );
        if (!isDefault) {
            performUpdate(authUser.getUid(), locationToSave);
            return;
        }
        firebaseLocationManager.hasDefaultLocation(authUser.getUid(),locationId, (success, hasDefault) -> {
            if (success) {
                if (hasDefault) {
                    Toast.makeText(this, "Địa chỉ mặc định đã tồn tại. Vui lòng tắt tùy chọn này hoặc chỉnh sửa địa chỉ mặc định cũ.", Toast.LENGTH_LONG).show();
                    return;
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
                Intent intent1 = new Intent(EditRecipientActivity.this, ChooseRecipientActivity.class);
                startActivity(intent1);
                finish();
            } else {
                Toast.makeText(EditRecipientActivity.this, "❌ Lỗi cập nhật: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteRecipient() {
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        final String locationIdToDelete = locationId;
        if (locationIdToDelete == null || locationIdToDelete.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ID địa chỉ để xóa.", Toast.LENGTH_SHORT).show();
            return;
        }
        firebaseLocationManager.deleteLocationById(authUser.getUid(), locationIdToDelete, (isSuccess, message) -> {
            if (isSuccess) {
                setResult(DELETE_RESULT_CODE, new Intent());
                finish();
            } else {
                Toast.makeText(EditRecipientActivity.this, "❌ Lỗi xóa: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}