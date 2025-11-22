package vn.haui.android_project.entity;

import java.util.List;
import java.util.Map;

public class Order {

    private String orderId;
    private String note;
    private String status;
    private String timeDisplay;

    private int deliveryFee;
    private int discount;
    private int subTotal;
    private Double total;

    private UserLocationEntity addressUser;
    private Map<String, String> delivery;
    private List<ItemOrderProduct> productList;
    private Map<String,Object> receiver;
    private Map<String,Object>  shipper;
    private Map<String,Object>  store;

    private PaymentCard paymentCard;

    private String uid;

    public PaymentCard getPaymentCard() {
        return paymentCard;
    }

    public void setPaymentCard(PaymentCard paymentCard) {
        this.paymentCard = paymentCard;
    }

    public Order() {}

    // GETTER + SETTER

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimeDisplay() {
        return timeDisplay;
    }

    public void setTimeDisplay(String timeDisplay) {
        this.timeDisplay = timeDisplay;
    }

    public int getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(int deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

    public int getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(int subTotal) {
        this.subTotal = subTotal;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public UserLocationEntity getAddressUser() {
        return addressUser;
    }

    public void setAddressUser(UserLocationEntity addressUser) {
        this.addressUser = addressUser;
    }

    public Map<String, String> getDelivery() {
        return delivery;
    }

    public void setDelivery(Map<String, String> delivery) {
        this.delivery = delivery;
    }

    public List<ItemOrderProduct> getProductList() {
        return productList;
    }

    public void setProductList(List<ItemOrderProduct> productList) {
        this.productList = productList;
    }

    public Map<String, Object> getReceiver() {
        return receiver;
    }

    public void setReceiver(Map<String, Object> receiver) {
        this.receiver = receiver;
    }

    public Map<String, Object> getShipper() {
        return shipper;
    }

    public void setShipper(Map<String, Object> shipper) {
        this.shipper = shipper;
    }

    public Map<String, Object> getStore() {
        return store;
    }

    public void setStore(Map<String, Object> store) {
        this.store = store;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

}
