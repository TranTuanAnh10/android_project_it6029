package vn.haui.android_project.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import vn.haui.android_project.R;
import vn.haui.android_project.databinding.OrderDetailScreenBinding;

public class OrderDetailsActivity extends AppCompatActivity {

    private OrderDetailScreenBinding binding;
    private static final String TAG = "OrderDetailsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = OrderDetailScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        displayOrderDetails();
        setupListeners();
    }


    private void displayOrderDetails() {
        // Cập nhật thông tin tổng quan
        binding.tvEstimateTime.setText("10:10");
        binding.tvOrderId.setText("Order ID: CA321457");
        binding.tvStatusTag.setText("Driver is picking up your Order.");

        // Cập nhật thông tin tài xế
        binding.tvDriverName.setText("Adam West");
        binding.tvLicensePlate.setText("0981094505");

        // Cập nhật Tóm tắt tài chính
        binding.tvDeliveryFeeValue.setText("$0");
        binding.tvDiscountValue.setText("- $15");
        binding.tvTotalValue.setText("$115");

      }

    private void setupListeners() {

        // Nút Hủy đơn hàng
        binding.btnCancelOrder.setOnClickListener(v -> {
            Log.d(TAG, "User clicked Cancel Order");
            // Thực hiện logic hủy đơn hàng (hiển thị dialog xác nhận, gọi API)
            showConfirmationDialog("Bạn có chắc muốn hủy đơn hàng này không?");
        });

        binding.btnChat.setOnClickListener(view -> {
            try {
                // Lấy số điện thoại từ TextView thông qua binding
                String phoneNumber = binding.tvLicensePlate.getText().toString();
                // Tạo Intent để mở ứng dụng nhắn tin
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phoneNumber));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Không thể mở ứng dụng nhắn tin", Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnCall.setOnClickListener(view -> {
            try {
                // Lấy số điện thoại từ TextView thông qua binding
                String phoneNumber = binding.tvLicensePlate.getText().toString();
                // Tạo Intent để mở ứng dụng gọi điện
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phoneNumber));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Không thể thực hiện cuộc gọi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTimelineStatus(boolean isOrderReady) {
        if (isOrderReady) {
            // Thay đổi icon thành một icon khác, ví dụ: một dấu tick màu xanh
//            binding.itemTimelinePrepared.timelinePreparedIcon.setImageResource(R.drawable.ic_prepared_order);
        } else {
            // Giữ nguyên hoặc đặt một icon mặc định
//            binding.itemTimelinePrepared.timelinePreparedIcon.setImageResource(R.drawable.ic_prepared_order_active);
        }
//         binding.timelinePreparedLayout.timelinePreparedLine.setBackgroundColor(getColor(R.color.some_color));
    }
    private void showConfirmationDialog(String message) {
        // Ở đây cần triển khai DialogFragment hoặc AlertDialog tùy chỉnh.
        // Ví dụ đơn giản:
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận hủy")
                .setMessage(message)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    // Logic hủy đơn hàng
                    Toast.makeText(this, "Đơn hàng đã được yêu cầu hủy.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Không", null)
                .show();
    }
}
