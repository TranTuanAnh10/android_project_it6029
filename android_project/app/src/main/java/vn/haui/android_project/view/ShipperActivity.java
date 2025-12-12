package vn.haui.android_project.view;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.android_project.R;
import vn.haui.android_project.adapter.ShipperOrderAdapter;
import vn.haui.android_project.entity.NotificationEntity;
import vn.haui.android_project.entity.ProductItem;
import vn.haui.android_project.entity.UserEntity;
import vn.haui.android_project.enums.DatabaseTable;
import vn.haui.android_project.enums.MyConstant;
import vn.haui.android_project.enums.UserRole;
import vn.haui.android_project.model.OrderShiperHistory;
import vn.haui.android_project.services.FirebaseNotificationService;
import vn.haui.android_project.services.LocationService;

public class ShipperActivity extends AppCompatActivity {
    private Spinner spinnerStatus;
    private String currentUserId;
    private Button btnDatePicker;
    private Calendar myCalendar;
    private AlertDialog currentOrderDialog;
    private RecyclerView recyclerViewOrders;
    private ShipperOrderAdapter shipperOrderAdapter;
    private List<OrderShiperHistory> listOrder = new ArrayList<>();
    private DatabaseReference notiRef;
    private LocationService locationService;
    private DatabaseReference mDatabase;
    Toolbar toolbar;
    BottomNavigationView bottomNavigationView;
    FrameLayout container;
    private FirebaseNotificationService notificationService;
    private String nameShipper, phoneShipper, emailShipper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shipper);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Intent intent = getIntent();
        nameShipper = intent.getStringExtra("USER_NAME");
        phoneShipper = intent.getStringExtra("USER_PHONE");
        emailShipper = intent.getStringExtra("USER_EMAIL");

        notificationService = new FirebaseNotificationService();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        container = findViewById(R.id.container);
        locationService = new LocationService(this);
        listenForNewOrders();
        setupCustomerUI();
    }

    private void setupCustomerUI() {
        bottomNavigationView.setVisibility(View.VISIBLE);
        container.setVisibility(View.VISIBLE);


        getSupportFragmentManager().beginTransaction().replace(R.id.container, new HomeFragment()).commit();
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new ShipperOrderFragment()).commit();
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.btn_home) {
                selectedFragment = new ShipperOrderFragment();
            } else if (itemId == R.id.btn_order) {
                selectedFragment = new ShipperCalendarFragment();
            } else if (itemId == R.id.btn_notification) {
                selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.btn_profile) {
                selectedFragment = new ProfileFragment();
            }
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, selectedFragment).addToBackStack(null).commit();
            }
            return true;
        });
    }



    private void listenForNewOrders() {
        notiRef = FirebaseDatabase.getInstance().getReference("Notifications").child(currentUserId);

        notiRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String content = snapshot.child("content").getValue(String.class);
                    String orderId = snapshot.child("orderId").getValue(String.class);
                    String notiKey = snapshot.getKey(); // Key để xóa thông báo sau này

                    // Hiển thị Dialog
                    showNewOrderDialog(title, content, orderId, notiKey);
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String s) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String s) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showNewOrderDialog(String title, String content, String orderId, String notiKey) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setCancelable(false)
                .setPositiveButton("NHẬN ĐƠN", (dialog, which) -> {
                    // Xử lý nhận đơn (Quan trọng)
                    locationService.getCurrentLocation(new LocationService.LocationCallbackListener() {
                        @Override
                        public void onLocationResult(double la, double log, String add) {
                            LoadDataUser(orderId, notiKey, la, log);
                        }
                        @Override
                        public void onLocationError(String errorMessage) {
                            Toast.makeText(ShipperActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });

                })
                .setNegativeButton("ĐỂ SAU", (dialog, which) -> {
                    notiRef.child(notiKey).removeValue();
                    dialog.dismiss();
                })
                .show();
    }

    private void LoadDataUser(String orderId, String notiKey, double la, double log){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection(DatabaseTable.USERS.getValue()).document(currentUserId);
        userRef.get()
                .addOnSuccessListener(snapshot -> {
                    UserEntity userFirebase = snapshot.toObject(UserEntity.class);
                    processOrderAcceptance(orderId, notiKey, la, log, userFirebase.getName(), userFirebase.getPhoneNumber(), userFirebase.getEmail())
;                })
                .addOnFailureListener(exception -> {

                });
    }
    private void processOrderAcceptance(String orderId, String notiKey, double la, double log, String userName, String phone, String email) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);

        orderRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() == null) {
                    return Transaction.success(currentData);
                }
                String status = currentData.child("status").getValue(String.class);
                if (status != null && status.equals(MyConstant.PICKINGUP) && !currentData.child("shipper").hasChild("shipperId")) {
                    MutableData shipperNode = currentData.child("shipper");


                    shipperNode.child("lat").setValue(la);
                    shipperNode.child("lng").setValue(log);
//                    shipperNode.child("lat").setValue(21.053736);
//                    shipperNode.child("lng").setValue(105.7325319);

                    Map<String, Object> shipperInfoMap = new HashMap<>();
                    shipperInfoMap.put("shipperId", currentUserId);
                    shipperInfoMap.put("shipperName", userName);
                    shipperInfoMap.put("shipperPhone", phone);
                    shipperInfoMap.put("shipperEmail", email);
                    shipperNode.child("shipperInfo").setValue(shipperInfoMap);

                    currentData.child("status").setValue(MyConstant.DELIVERING);

                    return Transaction.success(currentData);
                } else {
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

                notiRef.child(notiKey).removeValue();

                if (committed) {
                    Toast.makeText(ShipperActivity.this, "Nhận đơn thành công!", Toast.LENGTH_SHORT).show();
                    FirebaseDatabase.getInstance().getReference("shippers")
                            .child(currentUserId).child("status").setValue("busy");
                    DatabaseReference uidRef = FirebaseDatabase.getInstance().getReference("orders")
                            .child(orderId).child("uid");
                    uidRef.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DataSnapshot snapshot = task.getResult();
                            if (snapshot.exists()) {
                                String uid = snapshot.getValue(String.class);
                                sendOrderSuccessNotification(orderId, uid);
                            } else {
                                Toast.makeText(ShipperActivity.this, "Không tìm thấy UID cho orderId: " + orderId, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ShipperActivity.this, "Lỗi khi lấy UID: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    saveToShipperHistory(currentData, orderId);
                } else {
                    Toast.makeText(ShipperActivity.this, "Đơn đã có người nhận.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void sendOrderSuccessNotification(String orderId, String uidUserOrder) {
        String msg = "Shipper " + nameShipper + " đã nhận đơn hàng #" + orderId + " của bạn.";
        String body = "Hãy theo dõi hành trình đơn hàng nhé!";
        NotificationEntity notification = new NotificationEntity(
                "Đơn hàng",
                msg,
                body,
                "ORDER_STATUS"
        );
        notification.setImageUrl(null);
        notification.setTargetId(orderId);
        notificationService.addNotification(uidUserOrder, notification);
    }
    private void saveToShipperHistory(DataSnapshot orderSnapshot, String orderId) {
        String price = String.valueOf(orderSnapshot.child("total").getValue());
        if (price.equals("null")) price = "0";

        DataSnapshot addressNode = orderSnapshot.child("addressUser");

        String name = addressNode.child("recipientName").getValue(String.class);
        String phone = addressNode.child("phoneNumber").getValue(String.class);
        String address = addressNode.child("address").getValue(String.class);
        String currentDate = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());

        OrderShiperHistory historyItem = new OrderShiperHistory(
                orderId,
                "shipping",
                name,
                phone,
                address,
                price,
                currentDate
        );

        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference("shippers")
                .child(currentUserId)
                .child("historys");

        historyRef.child(orderId).setValue(historyItem);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notiRef != null) {

        }
    }
}