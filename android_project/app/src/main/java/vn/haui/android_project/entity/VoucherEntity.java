package vn.haui.android_project.entity;

import com.google.firebase.database.PropertyName;

import java.io.Serializable;
import java.util.Map;

public class VoucherEntity implements Serializable {
    private String id;
    private String name;            // Tên voucher (ví dụ: Giảm giá mùa hè)
    private String code;            // Mã voucher (ví dụ: SUMMER2024)
    private String imageUrl;        // Link ảnh voucher

    private long expiryDate;        // Hạn sử dụng (Timestamp)
    private boolean isActive;       // Trạng thái: true (còn dùng được), false (đã khóa/hết hạn)

    private String discountType;    // Loại giảm giá: "PERCENT" (Phần trăm) hoặc "AMOUNT" (Tiền mặt)
    private double discountValue;   // Giá trị giảm (Ví dụ: 20 nếu là %, 50000 nếu là tiền)
    private double minOrderValue;   // Giá trị đơn hàng tối thiểu để áp dụng
    private double maxOrderValue;   // Số tiền giảm tối đa (nếu có)
    private String description; // mô tả

    public VoucherEntity() {
        // Constructor rỗng bắt buộc cho Firebase
    }

    public VoucherEntity(String id, String name, String code, String imageUrl,
                         long expiryDate, boolean isActive, String discountType,
                         double discountValue, double minOrderValue, double maxOrderValue, String description) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.imageUrl = imageUrl;
        this.expiryDate = expiryDate;
        this.isActive = isActive;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxOrderValue = maxOrderValue;
        this.minOrderValue = minOrderValue;
        this.description = description;
    }

    // --- Helper Method: Kiểm tra Voucher còn hiệu lực không ---
    public boolean isValid() {
        long currentTime = System.currentTimeMillis();
        return isActive && (expiryDate > currentTime);
    }

    public double getMaxOrderValue() {
        return maxOrderValue;
    }

    public void setMaxOrderValue(double maxOrderValue) {
        this.maxOrderValue = maxOrderValue;
    }

    // --- Getter & Setter ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }

    @PropertyName("active") // Map với field "active" trên Firebase
    public boolean isActive() {
        return isActive;
    }

    @PropertyName("active")
    public void setActive(boolean active) {
        isActive = active;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public double getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(double minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Hàm hỗ trợ map từ Firebase (nếu cần dùng tay thay vì để Firebase tự map)
    public static VoucherEntity fromMap(Map<String, Object> map) {
        VoucherEntity v = new VoucherEntity();
        if (map == null) return v;
        v.id = (String) map.get("id");
        v.name = (String) map.get("name");
        v.code = (String) map.get("code");
        v.imageUrl = (String) map.get("imageUrl");
        v.discountType = (String) map.get("discountType");

        Object activeObj = map.get("active");
        v.isActive = activeObj != null ? (Boolean) activeObj : false;

        Object expObj = map.get("expiryDate");
        if (expObj instanceof Number) v.expiryDate = ((Number) expObj).longValue();

        Object valObj = map.get("discountValue");
        if (valObj instanceof Number) v.discountValue = ((Number) valObj).doubleValue();

        Object minObj = map.get("minOrderValue");
        if (minObj instanceof Number) v.minOrderValue = ((Number) minObj).doubleValue();
        v.discountType = (String) map.get("description");
        return v;
    }
}
