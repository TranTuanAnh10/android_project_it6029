package vn.haui.android_project.services;

import vn.haui.android_project.entity.Order;

public interface OrderCallback {
    void onOrderLoaded(Order order);
    void onError(Exception e);
}
