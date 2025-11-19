package vn.haui.android_project.view;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import vn.haui.android_project.R;

public class OrderDetailManagementActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Bạn cần tạo layout activity_order_detail_management.xml cho file này
        setContentView(R.layout.activity_order_detail_management);

        String orderId = getIntent().getStringExtra("ORDER_ID");
        // TODO: Load chi tiết đơn hàng dựa vào orderId
    }
}
