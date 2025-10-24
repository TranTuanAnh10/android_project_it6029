package vn.haui.android_project.entity;

import java.util.List;

public class Store {
    private String storeName;
    private int imageRes;
    private List<FoodItem> items;

    private boolean expanded; // ✅ thêm dòng này


    public Store(String storeName, int imageRes, List<FoodItem> items, boolean expanded) {
        this.storeName = storeName;
        this.imageRes = imageRes;
        this.items = items;
        this.expanded = true; // mặc định mở

    }

    public String getStoreName() { return storeName; }
    public int getImageRes() { return imageRes; }
    public List<FoodItem> getItems() { return items; }
    public boolean isExpanded() { return expanded; }


    public void setExpanded(boolean expanded) { this.expanded = expanded; }

}
