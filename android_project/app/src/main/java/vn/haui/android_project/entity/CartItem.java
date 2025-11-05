package vn.haui.android_project.entity;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties // Bỏ qua các trường lạ (nếu có)
public class CartItem {

    // Tên biến phải khớp chính xác với key trong JSON RTDB
    public ProductItem item_details;
    public Object quantity;

    public CartItem() {}

    public CartItem(ProductItem itemDetails, int quantity) {
        this.item_details = itemDetails;
        this.quantity = quantity;
    }
    public int getQuantity(){
        return Integer.parseInt(quantity.toString());
    }

    public String quantityToString(){
        return quantity.toString();
    }

}
