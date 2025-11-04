package vn.haui.android_project.entity;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties // Bỏ qua các trường lạ (nếu có)
public class CartItem {

    // Tên biến phải khớp chính xác với key trong JSON RTDB
    public ProductItem item_details;
    public int quantity;

    // Cần constructor rỗng
    public CartItem() {}

    public CartItem(ProductItem itemDetails, int quantity) {
        this.item_details = itemDetails;
        this.quantity = quantity;
    }
    public String getQuantity(){
        return quantity + "";
    }

    // (Bạn không bắt buộc cần getter/setter nếu dùng public fields)
}
