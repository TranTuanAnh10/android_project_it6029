package vn.haui.android_project.entity;

public class OrderProduct {
    private String name;
    private String details;
    private int quantity;
    private double unitPrice; // Giá cho 1 đơn vị

    public OrderProduct(String name, String details, int quantity, double unitPrice) {
        this.name = name;
        this.details = details;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // Getters
    public String getName() { return name; }
    public String getDetails() { return details; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }

    // Tổng giá = Giá đơn vị * Số lượng
    public double getTotalPrice() { return unitPrice * quantity; }
}