package vn.haui.android_project.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast; // Thêm Toast để xử lý thông báo lỗi (nếu cần)

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.haui.android_project.MainActivity;
import vn.haui.android_project.R;

public class OrderPlacedActivity extends AppCompatActivity {


    private TextView tvOrderId;
    private Button btnReturnHome, btnTrackOrder;
    FirebaseAuth mAuth;
    private  String orderId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order_placed);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_order_placed), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mapViews();
        Intent intent = getIntent();

        if (intent != null) {
            orderId = intent.getStringExtra("ORDER_ID");
            tvOrderId.setText(orderId);
        }
        // Khởi tạo FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

//        btnReturnHome.setOnClickListener(v -> {
//            FirebaseUser user = mAuth.getCurrentUser();
//            Intent intent = new Intent(OrderPlacedActivity.this, MainActivity.class);
//            if (user != null) {
//                intent.putExtra("USER_ID", user.getUid());
//                intent.putExtra("USER_EMAIL", user.getEmail());
//                intent.putExtra("USER_NAME", user.getDisplayName());
//            }
//            startActivity(intent);
//            finish();
//        });
        if (btnTrackOrder != null) {
            btnTrackOrder.setOnClickListener(v -> {
                Intent intent1 = new Intent(OrderPlacedActivity.this, OrderDetailsActivity.class);
                intent1.putExtra("ORDER_ID", orderId);
                startActivity(intent1);
                finish();
            });
        }
    }

    private void mapViews() {
        tvOrderId = findViewById(R.id.tv_order_id);
        // Lưu ý: Đảm bảo ID trong XML là R.id.btnReturnHome, nếu không phải thì thay bằng R.id.btn_return_home
        btnReturnHome = findViewById(R.id.btn_return_home);
        btnTrackOrder = findViewById(R.id.btn_track_order);
    }
}