package vn.haui.android_project.entity;

import java.io.Serializable;
import java.util.List;

public class Cart implements Serializable {
    public List<CartItem> items;

    public Cart(){

    }
    public Cart(List<CartItem> items) {
        this.items = items;
    }
}
