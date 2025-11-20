package vn.haui.android_project.view;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import vn.haui.android_project.R;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.enums.MyConstant;

public class OrderPlacedActivity extends AppCompatActivity {


    private TextView tvOrderId;
    private Button btnReturnHome, btnTrackOrder;
    FirebaseAuth mAuth;
    private String orderId, status;
    private DatabaseReference orderRef;
    private FirebaseDatabase firebaseDatabase;

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
        FirebaseApp.initializeApp(this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        orderRef = firebaseDatabase.getReference(DatabaseTable.ORDERS.getValue()).child(orderId);
        listenOrderRealtime();
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
                if (MyConstant.PREPARED.equals(status) || MyConstant.PICKINGUP.equals(status)) {
                    Intent intent1 = new Intent(OrderPlacedActivity.this, OrderDetailsActivity.class);
                    intent1.putExtra("ORDER_ID", orderId);
                    startActivity(intent1);
                    finish();
                } else if (MyConstant.DELIVERING.equals(status)) {
                    Intent intent1 = new Intent(OrderPlacedActivity.this, OrderTrackingActivity.class);
                    intent1.putExtra("ORDER_ID", orderId);
                    startActivity(intent1);
                    finish();
                }
//                else if (MyConstant.FINISH.equals(status)){
//                    Intent intent1 = new Intent(OrderPlacedActivity.this, MainActivity.class);
//                    startActivity(intent1);
//                    finish();
//                }
            });
        }
    }

    private void listenOrderRealtime() {
        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                orderId = snapshot.child("orderId").getValue(String.class);
                status = snapshot.child("status").getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase read failed: " + error.getMessage());
            }
        });
    }

    private void mapViews() {
        tvOrderId = findViewById(R.id.tv_order_id);
        // Lưu ý: Đảm bảo ID trong XML là R.id.btnReturnHome, nếu không phải thì thay bằng R.id.btn_return_home
        btnReturnHome = findViewById(R.id.btn_return_home);
        btnTrackOrder = findViewById(R.id.btn_track_order);
    }
}