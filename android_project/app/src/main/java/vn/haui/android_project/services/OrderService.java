package vn.haui.android_project.services;

import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import vn.haui.android_project.entity.Order;
import vn.haui.android_project.enums.MyConstant;

public class OrderService {
    public static OrderService getInstance(){
        return new OrderService();
    }

    private FirebaseAuth mAuth;


    public Order getOrderByOrderId(String orderId){
        mAuth = FirebaseAuth.getInstance();
        List<Order> orders = new ArrayList<>();
        Order targetOrder = new Order();

        if (mAuth == null || mAuth.getCurrentUser() == null) {
            try {
                throw new Exception("Bạn chưa đăng nhập!");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        //String userId = mAuth.getCurrentUser().getUid();

        DatabaseReference orderRef = FirebaseDatabase.getInstance()
                .getReference("orders");

        orderRef.get().addOnSuccessListener(snapshot -> {

            for (DataSnapshot child : snapshot.getChildren()) {
                Order order = child.getValue(Order.class);

                if (order != null ) {
                    orders.add(order);
                }
            }
            orders.forEach(order -> {
                if(order.getOrderId().equals(orderId)){
                    targetOrder.setOrderId(order.getOrderId());
                    targetOrder.setNote(order.getNote());
                    targetOrder.setStatus(order.getStatus());
                    targetOrder.setTimeDisplay(order.getTimeDisplay());
                    targetOrder.setDeliveryFee(order.getDeliveryFee());
                    targetOrder.setDiscount(order.getDiscount());
                    targetOrder.setSubTotal(order.getSubTotal());
                    targetOrder.setTotal(order.getTotal());
                    targetOrder.setAddressUser(order.getAddressUser());
                    targetOrder.setDelivery(order.getDelivery());
                    targetOrder.setProductList(order.getProductList());
                    targetOrder.setReceiver(order.getReceiver());
                    targetOrder.setShipper(order.getShipper());
                    targetOrder.setStore(order.getStore());
                    targetOrder.setPaymentCard(order.getPaymentCard());
                }
            });
        });

        return (targetOrder.getOrderId() != null)?targetOrder: null;
    }

}
