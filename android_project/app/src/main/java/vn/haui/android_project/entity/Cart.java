package vn.haui.android_project.entity;

import java.util.List;

public class Cart {
    public List<CartItem> items;

    public Cart(){

    }
    public Cart(List<CartItem> items) {
        this.items = items;
    }
}
