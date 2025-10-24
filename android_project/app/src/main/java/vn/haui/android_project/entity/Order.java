package vn.haui.android_project.entity;

public class Order {
    private String storeName;
    private String items;
    private String estimate;
    private String status;
    private String price;
    private int imageRes;

    public Order(String storeName, String items, String estimate, String status, String price, int imageRes) {
        this.storeName = storeName;
        this.items = items;
        this.estimate = estimate;
        this.status = status;
        this.price = price;
        this.imageRes = imageRes;
    }

    // Getters
    public String getStoreName() { return storeName; }
    public String getItems() { return items; }
    public String getEstimate() { return estimate; }
    public String getStatus() { return status; }
    public String getPrice() { return price; }
    public int getImageRes() { return imageRes; }
}
