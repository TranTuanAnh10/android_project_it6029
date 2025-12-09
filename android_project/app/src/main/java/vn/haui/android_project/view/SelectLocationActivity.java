package vn.haui.android_project.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar; // Thêm import cho ProgressBar
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import vn.haui.android_project.R;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.model.AddressAdapter;
import vn.haui.android_project.model.LocationItem;
import vn.haui.android_project.services.LocationService;

public class SelectLocationActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final long DELAY = 800; // 0.8 giây sau khi ngừng gõ mới call API
    private final Handler handler = new Handler();

    private EditText etSearchAddress;
    private Button btnUseMyLocation;
    private LinearLayout btnGoToMap;
    private ListView listRecent;
    private ProgressBar progressBarLoading; // Khai báo ProgressBar

    private String address, activityView;
    private double latitude, longitude;

    private LocationService locationService;
    private ArrayAdapter<String> adapter;
    private AddressAdapter addressAdapter;
    private final List<String> addressList = new ArrayList<>();
    private final List<LocationItem> locationList = new ArrayList<>();

    private UserLocationEntity userLocation;
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_select_location);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.select_location_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mapping();
        setupIntentData();
        setupListeners();
    }

    private void mapping() {
        locationService = new LocationService(this);
        etSearchAddress = findViewById(R.id.etSearchAddress);
        btnUseMyLocation = findViewById(R.id.btnUseMyLocation);
        btnGoToMap = findViewById(R.id.btnChooseFromMap);
        listRecent = findViewById(R.id.listRecent);
        progressBarLoading = findViewById(R.id.progress_bar_location_loading); // Ánh xạ ProgressBar

        userLocation = new UserLocationEntity();
        addressAdapter = new AddressAdapter(this, addressList);
        listRecent.setAdapter(addressAdapter);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void setupIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            activityView = intent.getStringExtra("activityView");
            if ("updateChoose".equals(activityView)) {
                userLocation = intent.getParcelableExtra("locationToSave", UserLocationEntity.class);
                latitude = userLocation.getLatitude();
                longitude = userLocation.getLongitude();
                address = userLocation.getAddress();
                etSearchAddress.setText(address);
            } else {
                latitude = intent.getDoubleExtra("latitude", 0.0);
                longitude = intent.getDoubleExtra("longitude", 0.0);
                address = intent.getStringExtra("address");
                if (address != null) etSearchAddress.setText(address);
            }
        }
    }

    private void setupListeners() {
        // ✅ Khi bấm nút “Dùng vị trí của tôi”
        btnUseMyLocation.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            } else {
                getLocation();
            }
        });

        // ✅ Khi người dùng nhập từ khóa
        etSearchAddress.addTextChangedListener(new TextWatcher() {
            private Runnable searchRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                searchRunnable = () -> {
                    if (!query.isEmpty()) searchAddress(query);
                };
                handler.postDelayed(searchRunnable, DELAY);
            }
        });

        // ✅ Khi người dùng chọn 1 địa chỉ trong danh sách
        listRecent.setOnItemClickListener((parent, view, position, id) -> {
            LocationItem selected = locationList.get(position);
            address = selected.getName();
            latitude = selected.getLatitude();
            longitude = selected.getLongitude();
            etSearchAddress.setText(address);
        });
        btnGoToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SelectLocationActivity.this, MapLocationActivity.class);
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                intent.putExtra("address", address);
                intent.putExtra("activityView", activityView);
                userLocation.setLatitude(latitude);
                userLocation.setLongitude(longitude);
                userLocation.setAddress(address);
                intent.putExtra("locationToSave", userLocation);
                startActivity(intent);
            }
        });
    }

    private void getLocation() {
        locationService.getCurrentLocation(new LocationService.LocationCallbackListener() {
            @Override
            public void onLocationResult(double la, double log, String add) {
                latitude = la;
                longitude = log;
                address = add;
                etSearchAddress.setText(address);
            }

            @Override
            public void onLocationError(String errorMessage) {
                Toast.makeText(SelectLocationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchAddress(String query) {
        new AsyncTask<String, Void, List<LocationItem>>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                // 1. Hiển thị ProgressBar và ẩn ListView
                progressBarLoading.setVisibility(View.VISIBLE);
                listRecent.setVisibility(View.GONE);
                addressList.clear(); // Xóa kết quả cũ ngay lập tức
                addressAdapter.notifyDataSetChanged();
            }

            @Override
            protected List<LocationItem> doInBackground(String... params) {
                List<LocationItem> results = new ArrayList<>();
                try {
                    String encodedQuery = URLEncoder.encode(params[0], "UTF-8");
                    String apiKey = "9c71185187c2481896469de6f1006bf8"; // 🔑 thay bằng key thật
                    String apiUrl = "https://api.geoapify.com/v1/geocode/autocomplete?text="
                            + encodedQuery + "&limit=5&filter=countrycode:vn&apiKey=" + apiKey;

                    URL url = new URL(apiUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    JSONArray features = json.getJSONArray("features");

                    for (int i = 0; i < features.length(); i++) {
                        JSONObject properties = features.getJSONObject(i).getJSONObject("properties");
                        String formatted = properties.getString("formatted");
                        double lat = properties.getDouble("lat");
                        double lon = properties.getDouble("lon");

                        results.add(new LocationItem(formatted, lat, lon));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return results;
            }

            @Override
            protected void onPostExecute(List<LocationItem> result) {
                // 2. Ẩn ProgressBar
                progressBarLoading.setVisibility(View.GONE);

                // 3. Hiển thị lại ListView
                listRecent.setVisibility(View.VISIBLE);

                addressList.clear();
                locationList.clear();
                for (LocationItem item : result) {
                    addressList.add(item.getName());
                    locationList.add(item);
                }
                addressAdapter.notifyDataSetChanged();
                if (result.isEmpty()) {
                    Toast.makeText(SelectLocationActivity.this, "Không tìm thấy kết quả", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(query);
    }

    private double parseDoubleSafe(String value) {
        try {
            return value == null ? 0 : Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền vị trí để tiếp tục", Toast.LENGTH_SHORT).show();
            }
        }
    }
}