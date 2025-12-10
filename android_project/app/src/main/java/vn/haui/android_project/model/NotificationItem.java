package vn.haui.android_project.model;

import vn.haui.android_project.entity.NotificationEntity;

/**
 * Lớp này là một "View Model" cho Adapter.
 * Nó bao bọc (wraps) NotificationEntity và thêm thông tin về cách hiển thị.
 * Giúp Adapter biết được một mục là Header hay là một Item thông báo.
 */
public class NotificationItem {

    private final boolean isGroupHeader;
    private final String headerTitle;
    private final NotificationEntity entity;

    /**
     * Constructor riêng tư cho Header.
     * Chỉ có thể được tạo thông qua factory method asHeader().
     */
    private NotificationItem(String headerTitle) {
        this.isGroupHeader = true;
        this.headerTitle = headerTitle;
        this.entity = null; // Không có entity cho header
    }

    /**
     * Constructor riêng tư cho Item.
     * Chỉ có thể được tạo thông qua factory method asItem().
     */
    private NotificationItem(NotificationEntity entity) {
        this.isGroupHeader = false;
        this.headerTitle = null; // Không có tiêu đề cho item
        this.entity = entity;
    }

    // --- Factory Methods (Cách tạo đối tượng an toàn và rõ ràng) ---

    /**
     * Tạo một đối tượng NotificationItem đại diện cho một Header.
     * @param title Tiêu đề của nhóm (ví dụ: "Mới").
     * @return Một đối tượng NotificationItem kiểu Header.
     */
    public static NotificationItem asHeader(String title) {
        return new NotificationItem(title);
    }

    /**
     * Tạo một đối tượng NotificationItem đại diện cho một Item thông báo.
     * @param entity Đối tượng NotificationEntity gốc từ Firestore.
     * @return Một đối tượng NotificationItem kiểu Item.
     */
    public static NotificationItem asItem(NotificationEntity entity) {
        return new NotificationItem(entity);
    }


    // --- Getters ---

    public boolean isGroupHeader() {
        return isGroupHeader;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public NotificationEntity getEntity() {
        return entity;
    }
}
