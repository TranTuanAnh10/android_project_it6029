package vn.haui.android_project.callbacks;

import java.util.List;

import vn.haui.android_project.entity.NotificationEntity;
import vn.haui.android_project.entity.VoucherEntity;

public interface VoucherUserListCallback {

    void onComplete(List<String> notifications, Exception exception);
}
