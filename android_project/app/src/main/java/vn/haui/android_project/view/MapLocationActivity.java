package vn.haui.android_project.view;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log; // Sử dụng android.util.Log
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.services.FirebaseLocationManager;

public class MapLocationActivity extends AppCompatActivity {
    private WebView webViewMap;
    private double latitude, longitude;
    private String address, activityView;
    private Button btnConfirmLocation;
    private TextView tvDetailAddress;
    private ImageButton btn_back_local;
    private FirebaseLocationManager firebaseLocationManager;

    private UserLocationEntity userLocationEntity = new UserLocationEntity();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_location);

        // Ánh xạ View
        webViewMap = findViewById(R.id.webview_map);
        tvDetailAddress = findViewById(R.id.tv_detail_address);
        btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        btn_back_local = findViewById(R.id.btn_back_local);
        firebaseLocationManager = new FirebaseLocationManager();

        // Xử lý sự kiện click
        btnConfirmLocation.setOnClickListener(v -> handleConfirmLocation());
        btn_back_local.setOnClickListener(v -> finish());

        // Lấy dữ liệu từ Intent
        handleIntentData();

        // Cấu hình WebView
        setupWebView();
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent == null) return;

        activityView = intent.getStringExtra("activityView");

        if ("updateChoose".equals(activityView) && intent.hasExtra("locationToSave")) {
            // Trường hợp chỉnh sửa địa chỉ đã có
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                userLocationEntity = intent.getParcelableExtra("locationToSave", UserLocationEntity.class);
            } else {
                userLocationEntity = intent.getParcelableExtra("locationToSave");
            }
            if (userLocationEntity != null) {
                latitude = userLocationEntity.getLatitude();
                longitude = userLocationEntity.getLongitude();
                address = userLocationEntity.getAddress();
            }
        } else {
            // Trường hợp thêm mới hoặc xem vị trí
            latitude = intent.getDoubleExtra("latitude", 21.0285); // Tọa độ mặc định: Hà Nội
            longitude = intent.getDoubleExtra("longitude", 105.8542);
            address = intent.getStringExtra("address");
        }

        if (address != null) {
            tvDetailAddress.setText(address);
        }
    }

    private void setupWebView() {
        // Kích hoạt JavaScript
        webViewMap.getSettings().setJavaScriptEnabled(true);

        // QUAN TRỌNG: Cho phép WebView truy cập các tệp cục bộ (cần cho 'file:///android_asset')
        webViewMap.getSettings().setAllowFileAccess(true);
        webViewMap.getSettings().setAllowContentAccess(true);

        // Cho phép lưu trữ DOM để JavaScript hoạt động ổn định
        webViewMap.getSettings().setDomStorageEnabled(true);

        // Thêm cầu nối JavaScript
        webViewMap.addJavascriptInterface(new WebAppInterface(), "Android");

        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // QUAN TRỌNG: Chỉ gọi hàm JS sau khi trang đã tải xong hoàn toàn
                centerMapAtLocation(latitude, longitude, 17);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                // Ghi log lỗi để debug (sử dụng android.util.Log)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.e("WebViewError", "Error: " + error.getDescription() + " for URL: " + request.getUrl().toString());
                }
            }
        });

        // Tải file HTML từ thư mục assets
        webViewMap.loadUrl("file:///android_asset/map_selector.html");
    }

    public void centerMapAtLocation(double lat, double lng, int zoom) {
        // Dùng String.format với Locale.US để đảm bảo dấu thập phân là "."
        final String jsCode = String.format(java.util.Locale.US, "javascript:centerMapAtLocation(%.6f, %.6f, %d);", lat, lng, zoom);
        webViewMap.evaluateJavascript(jsCode, null);
    }

    // Cầu nối giữa JavaScript và Java
    public class WebAppInterface {
        // Constructor rỗng là đủ
        public WebAppInterface() {}

        @JavascriptInterface
        public void onLocationChanged(final double lat, final double lng) {
            // Chạy trên luồng UI chính để cập nhật View
            runOnUiThread(() -> {
                latitude = lat;
                longitude = lng;
                // Lưu ý: Địa chỉ chi tiết (tên đường, số nhà) cần được lấy thông qua
                // một dịch vụ Geocoding khác nếu muốn cập nhật realtime.
                // Ở đây, ta giữ lại địa chỉ cũ hoặc đợi người dùng xác nhận.
                // tvDetailAddress.setText(String.format(java.util.Locale.US, "Tọa độ: %.4f, %.4f", lat, lng));
            });
        }
    }

    private void handleConfirmLocation() {
        if ("addNew".equals(activityView)) {
            // Chuyển sang màn hình thêm người nhận mới
            Intent intent1 = new Intent(MapLocationActivity.this, AddRecipientActivity.class);
            intent1.putExtra("latitude", latitude);
            intent1.putExtra("longitude", longitude);
            intent1.putExtra("address", address); // Giữ lại địa chỉ ban đầu nếu không có geocoding
            startActivity(intent1);
            finish();
        } else if ("updateChoose".equals(activityView)) {
            // Chuyển sang màn hình chỉnh sửa người nhận
            Intent intent1 = new Intent(MapLocationActivity.this, EditRecipientActivity.class);

            // Cập nhật tọa độ và địa chỉ mới nhất từ bản đồ
            intent1.putExtra("location_id", userLocationEntity.getId());
            intent1.putExtra("recipientName", userLocationEntity.getRecipientName());
            intent1.putExtra("phoneNumber", userLocationEntity.getPhoneNumber());
            intent1.putExtra("address", address); // Giữ lại địa chỉ ban đầu nếu không có geocoding
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
            // Flow mặc định: Lưu địa chỉ mới cho người dùng
            saveLocationToFirebase();
        }
    }

    private void saveLocationToFirebase() {
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

        firebaseLocationManager.appendLocation(
                currentUid,
                newLocation,
                (success, msg) -> {
                    if (success) {
                        Toast.makeText(this, "Lưu địa chỉ thành công!", Toast.LENGTH_SHORT).show();
                        // Quay về MainActivity hoặc màn hình trước đó
                        // Chú ý: Việc chuyển về MainActivity và truyền lại toàn bộ thông tin user có thể không cần thiết
                        // Nếu MainActivity đã xử lý được việc user đăng nhập.
                        finish();
                    } else {
                        Toast.makeText(this, "Lỗi: " + msg, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }
}
