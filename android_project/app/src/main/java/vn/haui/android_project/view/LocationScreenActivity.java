package vn.haui.android_project.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import vn.haui.android_project.R;
import vn.haui.android_project.services.LocationService;

public class LocationScreenActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private Button btnSelectLocation;
    private LocationService locationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.location_screen);

        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        locationService = new LocationService(this);

        btnSelectLocation.setOnClickListener(v -> checkPermissionAndGetLocation());
    }

    private void checkPermissionAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            getLocation();
        }
    }

    private void getLocation() {
        locationService.getCurrentLocation(new LocationService.LocationCallbackListener() {
            @Override
            public void onLocationResult(double latitude, double longitude, String address) {
                Intent intent = new Intent(LocationScreenActivity.this, SelectLocationActivity.class);
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                intent.putExtra("address", address);
                startActivity(intent);
            }

            @Override
            public void onLocationError(String errorMessage) {
                Toast.makeText(LocationScreenActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
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
