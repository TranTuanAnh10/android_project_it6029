package vn.haui.android_project.entity;

import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class NotificationEntity implements Serializable {

    private String id;          // ID duy nhất của thông báo
    private String headerTitle;       // Tiêu đề
    private String title;       // Tiêu đề
    private String body;        // Nội dung
    private String type;        // Loại: "PROMOTION", "ORDER_STATUS", "SYSTEM"
    private boolean isRead;     // Trạng thái đã đọc
    @ServerTimestamp
    private Date createdAt;     // Thời gian tạo
    private String targetId;    // ID của đối tượng liên quan (OrderID, ProductID)
    private String imageUrl;    // URL hình ảnh (nếu có)
    private boolean isGroupHeader;
    public NotificationEntity() {
        // Constructor rỗng
    }
    public NotificationEntity(String headerTitle) {
        this.headerTitle = headerTitle;
        this.isGroupHeader = true;
    }
    // Constructor tiện lợi
    public NotificationEntity(String headerTitle,String title, String body, String type) {
        this.headerTitle=headerTitle;
        this.title = title;
        this.body = body;
        this.type = type;
        this.isRead = false; // Mặc định là chưa đọc
    }

    public boolean isGroupHeader() {
        return isGroupHeader;
    }

    public void setGroupHeader(boolean groupHeader) {
        isGroupHeader = groupHeader;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public void setHeaderTitle(String headerTitle) {
        this.headerTitle = headerTitle;
    }

    // Getters và Setters...
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Factory method để tạo đối tượng từ Map, giống như trong LocationManager.
     */
    public static NotificationEntity fromMap(Map<String, Object> map) {
        if (map == null) return null;
        NotificationEntity entity = new NotificationEntity();
        entity.setId((String) map.get("id"));
        entity.setTitle((String) map.get("title"));
        entity.setBody((String) map.get("body"));
        entity.setType((String) map.get("type"));
        if (map.get("read") instanceof Boolean) {
            entity.setRead((Boolean) map.get("read"));
        }
        if (map.get("createdAt") instanceof com.google.firebase.Timestamp) {
            entity.setCreatedAt(((com.google.firebase.Timestamp) map.get("createdAt")).toDate());
        }
        entity.setTargetId((String) map.get("targetId"));
        entity.setImageUrl((String) map.get("imageUrl"));
        return entity;
    }
}
