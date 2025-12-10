package vn.haui.android_project.enums;

public enum DatabaseTable {
    USERS("users"),
    USER_LOCATIONS("user_locations"),
    ORDERS("orders"),
    NOTIFICATIONS("notifications"),
    USER_PAYMENT_METHOD("user_payment_method"),
    VOUCHERS("vouchers"),
    VOUCHER_USER("voucher_user"),
   ;

    private final String value;

    DatabaseTable(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    // ✅ Dùng khi cần chuyển từ String trong Firebase về Enum
    public static DatabaseTable fromValue(String value) {
        for (DatabaseTable role : values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        return USERS; // mặc định
    }
}
