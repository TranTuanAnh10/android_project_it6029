package vn.haui.android_project;

import static android.view.View.GONE;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.haui.android_project.adapter.OrderItemsAdapter;
import vn.haui.android_project.entity.ItemOrderProduct;
import vn.haui.android_project.entity.UserLocationEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.enums.MyConstant;
import vn.haui.android_project.utils.TimeUtils;
import vn.haui.android_project.view.OrderDetailsActivity;
import vn.haui.android_project.view.OrderTrackingActivity;

public class ActivityRating extends AppCompatActivity {

    private ImageButton imageButtonBack;
    private String orderId;
    private DatabaseReference orderRef;
    private FirebaseDatabase firebaseDatabase;
    private OrderItemsAdapter orderItemsAdapter;
    private RecyclerView rvOrderItems;
    private List<ItemOrderProduct> productList = new ArrayList<>();
    private RatingBar restaurantRatingBar,driverRatingBar;
    private Button submitReviewButton;
    private EditText commentEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rating);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_rating), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mapping();
        rvOrderItems = findViewById(R.id.rv_order_items);
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        orderItemsAdapter = new OrderItemsAdapter(productList);
        rvOrderItems.setAdapter(orderItemsAdapter);
        imageButtonBack.setOnClickListener(v -> {
            finish();
        });
        Intent intent = getIntent();
        if (intent != null) {
            orderId = intent.getStringExtra("ORDER_ID");
            FirebaseApp.initializeApp(this);
            firebaseDatabase = FirebaseDatabase.getInstance();
            orderRef = firebaseDatabase.getReference(DatabaseTable.ORDERS.getValue()).child(orderId);
            listenOrderRealtime();
        }
        setInitialRating();
        submitReviewButton.setOnClickListener (v -> {
            submitReview();
        });
    }
    private void listenOrderRealtime() {
        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String orderId = snapshot.child("orderId").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                String driver= null;
//                DataSnapshot shipperSnap = snapshot.child("shipper");
//                Map<String, Object> shipperInfoMap = shipperSnap.child("shipperInfo").getValue(Map.class);
//                String driver= shipperInfoMap.get("shipperName").toString();
//                String shipperPhone = shipperInfoMap.get("shipperPhone").toString();
//                String shipperAvatar = shipperInfoMap.get("shipperAvatar").toString();
                DataSnapshot productListSnapshot = snapshot.child("productList");
                GenericTypeIndicator<List<ItemOrderProduct>> t = new GenericTypeIndicator<List<ItemOrderProduct>>() {
                };
                if (productListSnapshot.exists()) {
                    List<ItemOrderProduct> fetchedList = productListSnapshot.getValue(t);
                    if (fetchedList != null) {
                        productList.clear();
                        productList.addAll(fetchedList);

                    }
                }
                orderItemsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void submitReview() {
        float restaurantRating = restaurantRatingBar.getRating();
        float driverRating = driverRatingBar.getRating();
        String comment = commentEditText.getText().toString().trim();
        if (restaurantRating == 0f || driverRating == 0f) {
            Toast.makeText(this, "Vui lòng đánh giá cả Nhà hàng và Tài xế.", Toast.LENGTH_LONG).show(); // Đã thêm dấu chấm phẩy
            return;
        }
        if (orderRef == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID đơn hàng.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Tạo một đối tượng Map chứa dữ liệu đánh giá để gửi lên Firebase
        Map<String, Object> ratingData = new HashMap<>();
        // Sử dụng Double để lưu giá trị số thực trong Firebase
        ratingData.put("restaurantRating", (double) restaurantRating);
        ratingData.put("driverRating", (double) driverRating);
        ratingData.put("comment", comment);
        ratingData.put("timestamp", TimeUtils.getCreationTime());

        // Cập nhật dữ liệu vào nút con "ratings" trong đơn hàng hiện tại
        orderRef.child("ratings").setValue(ratingData)
                .addOnSuccessListener(aVoid -> {
                    // Đánh giá thành công
                    Toast.makeText(this, "Cảm ơn bạn đã gửi đánh giá!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Xử lý khi có lỗi xảy ra
                    Toast.makeText(this, "Lỗi gửi đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ActivityRating", "Firebase error: " + e.getMessage());
                });
        finish();
    }
    private void mapping() {
        imageButtonBack = findViewById(R.id.back_button);
        restaurantRatingBar = findViewById(R.id.restaurant_rating_bar);
        driverRatingBar = findViewById(R.id.driver_rating_bar);
        submitReviewButton= findViewById(R.id.submit_review_button);
        commentEditText= findViewById(R.id.comment_edit_text);
    }
    private float DEFAULT_RATING = 5.0f;
    private void setInitialRating() {
        restaurantRatingBar.setRating(DEFAULT_RATING);
        driverRatingBar.setRating(DEFAULT_RATING);
        submitReviewButton.setEnabled(true);
    }
}