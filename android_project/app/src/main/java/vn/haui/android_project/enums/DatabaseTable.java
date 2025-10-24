package vn.haui.android_project.enums;

public enum DatabaseTable {
    USERS("users"),
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
