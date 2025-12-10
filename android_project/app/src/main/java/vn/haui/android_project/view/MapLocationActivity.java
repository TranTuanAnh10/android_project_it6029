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

// Đã loại bỏ: implements CustomLocationDialog.LocationDialogListener
public class MapLocationActivity extends AppCompatActivity {
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

        // Thay đổi: Xử lý logic xác nhận trực tiếp khi nhấn nút, không qua Dialog
        btnConfirmLocation.setOnClickListener(v -> handleConfirmLocation());
        btn_back_local.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        if (intent != null) {
            latitude = intent.getDoubleExtra("latitude", 0.0);
            longitude = intent.getDoubleExtra("longitude", 0.0);
            address = intent.getStringExtra("address");
            activityView = intent.getStringExtra("activityView");
            if (address != null) tvDetailAddress.setText(address);
            if ("updateChoose".equals(activityView) && intent.hasExtra("locationToSave")) {
                userLocationEntity = intent.getParcelableExtra("locationToSave", UserLocationEntity.class);
                latitude = userLocationEntity.getLatitude();
                longitude = userLocationEntity.getLongitude();
                address = userLocationEntity.getAddress();
                tvDetailAddress.setText(address);
            }
        }
        setupWebView();
        // Giữ nguyên logic khởi tạo map
        centerMapAtLocation(latitude != 0.0 ? latitude : 32.986, longitude != 0.0 ? longitude : -96.756, 17);
    }

    private void setupWebView() {
        webViewMap.getSettings().setJavaScriptEnabled(true);
        webViewMap.getSettings().setDomStorageEnabled(true);
        // Đã cập nhật WebAppInterface để nhận thêm địa chỉ
        webViewMap.addJavascriptInterface(new WebAppInterface(), "Android");
        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Đảm bảo map được focus sau khi load
                centerMapAtLocation(latitude, longitude, 14);
            }
        });
        webViewMap.loadUrl("file:///android_asset/map_selector.html");
    }

    public void centerMapAtLocation(double lat, double lng, int zoom) {
        final String jsCode = String.format("javascript:centerMapAtLocation(%.6f, %.6f, %d);", lat, lng, zoom);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webViewMap.evaluateJavascript(jsCode, null);
        } else {
            webViewMap.loadUrl(jsCode);
        }
    }

    // Đã cập nhật: Nhận thêm 'addr' và cập nhật biến 'address' toàn cục
    public class WebAppInterface {
        @JavascriptInterface
        public void onLocationChanged(final double lat, final double lng, final String addr) {
            runOnUiThread(() -> {
                latitude = lat;
                longitude = lng;
                address = addr; // Cập nhật địa chỉ chi tiết
                tvDetailAddress.setText(addr); // Cập nhật TextView hiển thị
            });
        }
    }

    // Đã loại bỏ: showCustomLocationDialog()

    // Thay thế onLocationOptionChosen, xử lý logic ngay khi nhấn nút
    private void handleConfirmLocation() {
        if (null != activityView && "addNew".endsWith(activityView)) {
            // Chuyển sang màn hình thêm người nhận mới
            Intent intent1 = new Intent(MapLocationActivity.this, AddRecipientActivity.class);
            intent1.putExtra("latitude", latitude);
            intent1.putExtra("longitude", longitude);
            intent1.putExtra("address", address);
            startActivity(intent1);
            finish();
        } else if (null != activityView && "updateChoose".endsWith(activityView)) {
            // Chuyển sang màn hình chỉnh sửa người nhận
            Intent intent1 = new Intent(MapLocationActivity.this, EditRecipientActivity.class);

            // Cập nhật tọa độ và địa chỉ mới nhất từ bản đồ
            intent1.putExtra("location_id", userLocationEntity.getId());
            intent1.putExtra("recipientName", userLocationEntity.getRecipientName());
            intent1.putExtra("phoneNumber", userLocationEntity.getPhoneNumber());
            intent1.putExtra("address", address);
            intent1.putExtra("latitude", latitude);
            intent1.putExtra("longitude", longitude);

            // Giữ lại các thông tin khác của Entity
            intent1.putExtra("locationType", userLocationEntity.getLocationType());
            intent1.putExtra("defaultLocation", userLocationEntity.isDefaultLocation());
            intent1.putExtra("country", userLocationEntity.getCountry());
            intent1.putExtra("zipCode", userLocationEntity.getZipCode());

            startActivity(intent1);
            finish();
        } else {
            // Flow mặc định: Lưu địa chỉ mặc định mới cho người dùng
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
                    "Địa chỉ mặc định",
                    latitude,
                    longitude,
                    address != null ? address : "Địa chỉ không xác định",
                    authUser.getPhoneNumber(),
                    true,
                    "Other",
                    null,
                    null
            );

            // Logic lưu vào Firebase
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
    // Đã loại bỏ: onLocationOptionChosen(boolean isOkSelected)
}