package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.services.FirebaseLocationManager;

public class MapLocationActivity extends AppCompatActivity implements CustomLocationDialog.LocationDialogListener {
    private WebView webViewMap;
    private double latitude, longitude;
    private String address, activityView;
    private Button btnConfirmLocation;
    private TextView tvDetailAddress;
    ImageButton btn_back_local;
    private FirebaseLocationManager firebaseLocationManager;

    private UserLocationEntity userLocationEntity = new UserLocationEntity();

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_location);
        webViewMap = findViewById(R.id.webview_map);
        tvDetailAddress = findViewById(R.id.tv_detail_address);
        btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        btn_back_local = findViewById(R.id.btn_back_local);
        firebaseLocationManager = new FirebaseLocationManager();
        btnConfirmLocation.setOnClickListener(v -> showCustomLocationDialog());
        btn_back_local.setOnClickListener(v -> finish());
        Intent intent = getIntent();
        if (intent != null) {
            latitude = intent.getDoubleExtra("latitude", 0.0);
            longitude = intent.getDoubleExtra("longitude", 0.0);
            address = intent.getStringExtra("address");
            activityView = intent.getStringExtra("activityView");
            if (address != null) tvDetailAddress.setText(address);
            if ("updateChoose".equals(activityView) && intent.hasExtra("locationToSave")) {
                userLocationEntity = intent.getParcelableExtra("locationToSave",UserLocationEntity.class);
                latitude = userLocationEntity.getLatitude();
                longitude = userLocationEntity.getLongitude();
                address = userLocationEntity.getAddress();
                tvDetailAddress.setText(address);
            }
        }
        setupWebView();
        centerMapAtLocation(32.986, -96.756, 17);
    }

    private void setupWebView() {
        webViewMap.getSettings().setJavaScriptEnabled(true);
        webViewMap.getSettings().setDomStorageEnabled(true);
        webViewMap.addJavascriptInterface(new WebAppInterface(), "Android");
        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                centerMapAtLocation(latitude, longitude, 14);
            }
        });
        webViewMap.loadUrl("file:///android_asset/map_selector.html");
    }

    public void centerMapAtLocation(double lat, double lng, int zoom) {
        // Tạo chuỗi JavaScript để gọi hàm
        final String jsCode = String.format("javascript:centerMapAtLocation(%.6f, %.6f, %d);", lat, lng, zoom);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webViewMap.evaluateJavascript(jsCode, null);
        } else {
            webViewMap.loadUrl(jsCode);
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void onLocationChanged(final double lat, final double lng) {
            runOnUiThread(() -> {
                latitude = lat;
                longitude = lng;
            });
        }
    }


    // Hàm hiển thị Dialog Fragment
    private void showCustomLocationDialog() {
        CustomLocationDialog dialog = new CustomLocationDialog();
        // Dùng getSupportFragmentManager()
        dialog.show(getSupportFragmentManager(), CustomLocationDialog.TAG);
    }

    // --- TRIỂN KHAI PHƯƠNG THỨC TỪ INTERFACE ---
    @Override
    public void onLocationOptionChosen(boolean isOkSelected) {
        if (isOkSelected) {
            if ("addNew".endsWith(activityView)) {
                Intent intent1 = new Intent(MapLocationActivity.this, AddRecipientActivity.class);
                intent1.putExtra("latitude", latitude);
                intent1.putExtra("longitude", longitude);
                intent1.putExtra("address", address);
                startActivity(intent1);
                finish();
            } else if ("updateChoose".endsWith(activityView)) {
                Intent intent1 = new Intent(MapLocationActivity.this, EditRecipientActivity.class);
                intent1.putExtra("location_id", userLocationEntity.getId());
                intent1.putExtra("recipientName", userLocationEntity.getRecipientName());
                intent1.putExtra("phoneNumber", userLocationEntity.getPhoneNumber());
                intent1.putExtra("address", userLocationEntity.getAddress());
                intent1.putExtra("locationType", userLocationEntity.getLocationType());
                intent1.putExtra("defaultLocation", userLocationEntity.isDefaultLocation());
                intent1.putExtra("country", userLocationEntity.getCountry());
                intent1.putExtra("zipCode", userLocationEntity.getZipCode());
                intent1.putExtra("latitude", userLocationEntity.getLatitude());
                intent1.putExtra("longitude", userLocationEntity.getLongitude());
                startActivity(intent1);
                finish();
            } else {
                FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
                if (authUser == null) {
                    Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                String currentUid = authUser.getUid();
                if (currentUid.isEmpty() || latitude == 0.0 || longitude == 0.0) {
                    Toast.makeText(this, "Không có đủ dữ liệu (UID hoặc tọa độ) để lưu.", Toast.LENGTH_LONG).show();
                    return;
                }
                UserLocationEntity newLocation = new UserLocationEntity(
                        String.valueOf(System.currentTimeMillis()),
                        "Địa chỉ mặc định",
                        latitude,
                        longitude,
                        address != null ? address : "Địa chỉ không xác định",
                        authUser.getPhoneNumber(),
                        true,
                        "Other",
                        null,
                        null
                );
                List<UserLocationEntity> locationList = new ArrayList<>();
                locationList.add(newLocation); // ✅ thêm location hiện tại vào danh sách
                firebaseLocationManager.appendLocation(
                        currentUid,
                        newLocation,
                        (success, msg) -> {
                            if (success) {
                                Intent intent = new Intent(MapLocationActivity.this, MainActivity.class);
                                intent.putExtra("USER_ID", authUser.getUid());
                                intent.putExtra("USER_EMAIL", authUser.getEmail());
                                intent.putExtra("USER_NAME", authUser.getDisplayName());
                                if (authUser.getPhotoUrl() != null)
                                    intent.putExtra("USER_PHOTO", authUser.getPhotoUrl().toString());
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, "Lỗi: " + msg, Toast.LENGTH_LONG).show();
                            }
                        }
                );
            }
        }
    }
}

