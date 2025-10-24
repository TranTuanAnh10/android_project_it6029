package vn.haui.android_project.entity;

public class FoodItem {
    private String name, desc, price;
    private int imageRes;
    private int quantity;

    public FoodItem(String name, String desc, String price, int imageRes, int quantity) {
        this.name = name;
        this.desc = desc;
        this.price = price;
        this.imageRes = imageRes;
        this.quantity = quantity;
    }

    public String getName() { return name; }
    public String getDesc() { return desc; }
    public String getPrice() { return price; }
    public int getImageRes() { return imageRes; }
    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
}
