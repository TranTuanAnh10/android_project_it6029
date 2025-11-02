package vn.haui.android_project.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Service xử lý vị trí người dùng — có thể tái sử dụng ở nhiều màn hình
 */
public class LocationService {

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;

    public interface LocationCallbackListener {
        void onLocationResult(double latitude, double longitude, String address);
        void onLocationError(String errorMessage);
    }

    public LocationService(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Lấy vị trí hiện tại của người dùng
     */
    @SuppressLint("MissingPermission")
    public void getCurrentLocation(LocationCallbackListener listener) {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        handleLocation(location, listener);
                    } else {
                        // Nếu không có cache, lấy vị trí mới
                        requestNewLocationData(listener);
                    }
                })
                .addOnFailureListener(e ->
                        listener.onLocationError("Không thể lấy vị trí: " + e.getMessage()));
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData(LocationCallbackListener listener) {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000
        ).setMaxUpdates(1).build();

        fusedLocationClient.requestLocationUpdates(locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        if (locationResult == null) {
                            listener.onLocationError("Không có dữ liệu vị trí!");
                            return;
                        }
                        fusedLocationClient.removeLocationUpdates(this);

                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            handleLocation(location, listener);
                        } else {
                            listener.onLocationError("Không thể xác định vị trí hiện tại!");
                        }
                    }
                },
                Looper.getMainLooper());
    }

    /**
     * Chuyển đổi từ toạ độ sang địa chỉ
     */
    private void handleLocation(Location location, LocationCallbackListener listener) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        String address = getAddressFromLocation(lat, lon);
        listener.onLocationResult(lat, lon, address);
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Không xác định được địa chỉ";
    }
}
