package vn.haui.android_project.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import vn.haui.android_project.entity.Order;


public class OrderService {

    public static OrderService getInstance(){
        return new OrderService();
    }

    private FirebaseAuth mAuth;


    public Order getOrderByOrderId(String orderId, OrderCallback callback) {

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (mAuth == null || mAuth.getCurrentUser() == null) {
            callback.onError(new Exception("Bạn chưa đăng nhập!"));
            return null;
        }

        DatabaseReference orderRef = FirebaseDatabase.getInstance()
                .getReference("orders");

        orderRef.get().addOnSuccessListener(snapshot -> {

            Order target = null;

            for (DataSnapshot child : snapshot.getChildren()) {
                Order order = child.getValue(Order.class);

                if (order != null && orderId.equals(order.getOrderId())) {
                    target = order;
                    break;
                }
            }

            if (target != null) {
                callback.onOrderLoaded(target);
            } else {
                callback.onError(new Exception("Không tìm thấy đơn hàng!"));
            }

        }).addOnFailureListener(e -> {
            callback.onError(e);
        });
        return null;
    }


}
