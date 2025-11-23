package vn.haui.android_project.model;

import java.util.Date;

public class OrderShiperHistory{
    private String orderId;
    private String status;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String price;
    private String date;

    public OrderShiperHistory() {
    }

    public OrderShiperHistory(String orderId, String status, String receiverName, String receiverPhone, String receiverAddress, String price, String date) {
        this.orderId = orderId;
        this.status = status;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.price = price;
        this.date = date;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getReceiverAddress() { return receiverAddress; }
    public void setReceiverAddress(String receiverAddress) { this.receiverAddress = receiverAddress; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

}
