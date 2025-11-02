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

import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.services.FirebaseLocationManager;
import vn.haui.android_project.services.LocationService;

public class AddRecipientActivity extends AppCompatActivity {
    private EditText et_recipient_name, et_phone_number, et_address, et_country, et_zip_code;
    private Button btn_save;
    private ImageButton btnBack;
    private ChipGroup chipGroupLocationType;
    private Chip chipHome, chipWork, chipOther;
    private SwitchMaterial switchDefaultAddress;
    private FirebaseLocationManager firebaseLocationManager;

    private TextView chooseMap;
    private LocationService locationService;

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

        Intent intent1 = getIntent();
        if (intent1 != null) {
            latitude = intent1.getDoubleExtra("latitude", 0.0);
            longitude = intent1.getDoubleExtra("longitude", 0.0);
            address = intent1.getStringExtra("address");
            if (address != null) et_address.setText(address);
        }
        setupChipGroupListener();
        setupWebView();
        chooseMap.setOnClickListener(v -> {
            Intent intent = new Intent(AddRecipientActivity.this, SelectLocationActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("address", address);
            startActivity(intent);
        });
        btnBack.setOnClickListener(v -> finish());
        btn_save.setOnClickListener(v -> saveRecipient());
    }



    private void mapping() {
        webViewMap= findViewById(R.id.webview_map_add);
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
        locationService = new LocationService(this);
    }

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
        // Đảm bảo chipGroupLocationType không null
        if (chipGroupLocationType == null) return;

        chipGroupLocationType.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup chipGroup, @NonNull List<Integer> checkedIds) {
                if (checkedIds.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Vui lòng chọn loại địa điểm.", Toast.LENGTH_SHORT).show();
                }
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
        if (!isDefault) {
            performSave(authUser.getUid(), buildLocationEntity(false, updatedRecipientName, updatedPhoneNumber, updatedAddress, updatedLocationType, updatedCountry, updatedZipCode));
            return;
        }
        String finalUpdatedPhoneNumber = updatedPhoneNumber;
        firebaseLocationManager.hasDefaultLocation(authUser.getUid(),null, (success, hasDefault) -> {
            if (success) {
                if (hasDefault) {
                    Toast.makeText(this, "Đã có 1 địa chỉ mặc định khác. Vui lòng tắt tùy chọn này để lưu.", Toast.LENGTH_LONG).show();
                } else {
                    performSave(authUser.getUid(), buildLocationEntity(true, updatedRecipientName, finalUpdatedPhoneNumber, updatedAddress, updatedLocationType, updatedCountry, updatedZipCode));
                }
            } else {
                Toast.makeText(this, "Lỗi kiểm tra trạng thái mặc định. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            }
        });
    }
    private void setupWebView() {
        // Cài đặt cơ bản (Giữ nguyên)
        webViewMap.getSettings().setJavaScriptEnabled(true);
        webViewMap.getSettings().setDomStorageEnabled(true);
        webViewMap.addJavascriptInterface(new WebAppInterfaceAdd(), "Android");
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
        // Tạo chuỗi JavaScript để gọi hàm
        final String jsCode = String.format("javascript:centerMapAtLocation(%.6f, %.6f, %d);", lat, lng, zoom);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webViewMap.evaluateJavascript(jsCode, null);
        } else {
            webViewMap.loadUrl(jsCode);
        }
    }
    public class WebAppInterfaceAdd {
        @JavascriptInterface
        public void onLocationChanged(final double lat, final double lng) {
            runOnUiThread(() -> {
                latitude = lat;
                longitude = lng;
            });
        }
    }
    private UserLocationEntity buildLocationEntity(
            boolean isDefault, String recipientName, String phoneNumber, String address,
            String locationType, String country, String zipCode) {

        return new UserLocationEntity(
                String.valueOf(System.currentTimeMillis()),
                recipientName,
                latitude, longitude, // lat/lng
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
                        Intent intent = new Intent(this, ChooseRecipientActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(AddRecipientActivity.this, "❌ Lỗi thêm địa chỉ: " + msg, Toast.LENGTH_LONG).show();
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
}