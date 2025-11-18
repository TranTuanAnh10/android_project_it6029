package vn.haui.android_project.entity;

public class ItemOrderProduct {
    private String name;
    private String details;
    private int quantity;
    private double unitPrice;
    private String image;
    private String idItem;
    public ItemOrderProduct( String idItem,String name, String details, int quantity, double unitPrice, String image) {
        this.idItem=idItem;
        this.name = name;
        this.details = details;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.image = image;
    }

    public ItemOrderProduct() {
    }
    public String getIdItem() {
        return idItem;
    }

    public void setIdItem(String idItem) {
        this.idItem = idItem;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    // Getters
    public String getName() { return name; }
    public String getDetails() { return details; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }

    // Tổng giá = Giá đơn vị * Số lượng
    public double getTotalPrice() { return unitPrice * quantity; }
}