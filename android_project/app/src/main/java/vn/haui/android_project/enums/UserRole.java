package vn.haui.android_project.enums;

public enum UserRole {
    USER("user"),
    ADMIN("admin"),;

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    // ✅ Dùng khi cần chuyển từ String trong Firebase về Enum
    public static UserRole fromValue(String value) {
        for (UserRole role : values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        return USER; // mặc định
    }
}
