package vn.haui.android_project.callbacks;

import java.util.List;
import vn.haui.android_project.entity.NotificationEntity;

/**
 * Một callback đơn giản để trả về kết quả của việc lấy danh sách thông báo.
 */
public interface NotificationListCallback {
    /**
     * Được gọi khi thao tác lấy danh sách hoàn tất.
     * @param notifications Danh sách thông báo. Sẽ là một danh sách rỗng nếu có lỗi hoặc không có dữ liệu.
     * @param exception Đối tượng lỗi nếu có, hoặc null nếu thành công.
     */
    void onComplete(List<NotificationEntity> notifications, Exception exception);
}
