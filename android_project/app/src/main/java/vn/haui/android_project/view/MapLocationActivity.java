package vn.haui.android_project.view;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
    private String address;
    private Button btnConfirmLocation;
    private TextView tvDetailAddress;

    private FirebaseLocationManager firebaseLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_location);
        webViewMap = findViewById(R.id.webview_map);
        tvDetailAddress = findViewById(R.id.tv_detail_address);
        btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        firebaseLocationManager = new FirebaseLocationManager();
        btnConfirmLocation.setOnClickListener(v -> showCustomLocationDialog());
        Intent intent = getIntent();
        if (intent != null) {
            latitude = intent.getDoubleExtra("latitude", 0.0);
            longitude = intent.getDoubleExtra("longitude", 0.0);
            address = intent.getStringExtra("address");
            if (address != null) tvDetailAddress.setText(address);
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
                    latitude,
                    longitude,
                    address != null ? address : "Địa chỉ không xác định",
                    true
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

