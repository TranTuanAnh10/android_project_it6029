package vn.haui.android_project.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

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
 * Service xử lý vị trí người dùng — có thể tái sử dụng ở nhiều màn hình.
 * QUAN TRỌNG: Việc YÊU CẦU quyền (requestPermission) phải được thực hiện trong Activity/Fragment.
 */
public class LocationService {

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private static final String TAG = "LocationService";

    public interface LocationCallbackListener {
        void onLocationResult(double latitude, double longitude, String address);
        void onLocationError(String errorMessage);
    }

    public LocationService(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Kiểm tra xem quyền ACCESS_FINE_LOCATION đã được cấp chưa.
     */
    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Lấy vị trí hiện tại của người dùng.
     */
    @SuppressLint("MissingPermission")
    public void getCurrentLocation(LocationCallbackListener listener) {
        if (!isPermissionGranted()) {
            String errorMsg = "Quyền truy cập vị trí (ACCESS_FINE_LOCATION) chưa được cấp. Vui lòng cấp quyền trong Activity/Fragment gọi hàm này.";
            Log.e(TAG, errorMsg);
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
            listener.onLocationError(errorMsg);
            return;
        }

        // Quyền đã được cấp, tiếp tục lấy vị trí
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        handleLocation(location, listener);
                    } else {
                        // Nếu không có cache, lấy vị trí mới
                        requestNewLocationData(listener);
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMsg = "Không thể lấy vị trí từ FusedLocationClient: " + e.getMessage();
                    Log.e(TAG, errorMsg, e);
                    listener.onLocationError(errorMsg);
                });
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData(LocationCallbackListener listener) {
        if (!isPermissionGranted()) {
            // Không nên xảy ra nếu đã kiểm tra ở getCurrentLocation, nhưng thêm guard clause để an toàn
            String errorMsg = "Thiếu quyền khi yêu cầu update vị trí mới.";
            Log.e(TAG, errorMsg);
            listener.onLocationError(errorMsg);
            return;
        }

        // Cấu hình yêu cầu vị trí
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000
        ).setMaxUpdates(1).build();

        fusedLocationClient.requestLocationUpdates(locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        fusedLocationClient.removeLocationUpdates(this); // Ngưng nhận updates sau khi có kết quả

                        if (locationResult == null) {
                            listener.onLocationError("Không có dữ liệu vị trí được trả về!");
                            return;
                        }

                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            handleLocation(location, listener);
                        } else {
                            listener.onLocationError("Không thể xác định vị trí hiện tại sau khi request update!");
                        }
                    }
                },
                Looper.getMainLooper());
    }

    /**
     * Chuyển đổi từ toạ độ sang địa chỉ và trả kết quả về Listener.
     */
    private void handleLocation(Location location, LocationCallbackListener listener) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        String address = getAddressFromLocation(lat, lon);
        listener.onLocationResult(lat, lon, address);
    }

    /**
     * Lấy địa chỉ từ Vĩ độ và Kinh độ.
     */
    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            // Chỉ lấy 1 địa chỉ
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            Log.e(TAG, "Lỗi Geocoding: Không thể kết nối hoặc Geocoder lỗi.", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Lỗi Geocoding: Tọa độ không hợp lệ.", e);
        }
        return "Không xác định được địa chỉ";
    }
}