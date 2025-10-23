package vn.haui.android_project.entity;

public class DeviceToken {
    private String token;
    private String deviceModel;
    private String osVersion;
    private long createdAt;

    public DeviceToken() {}

    public DeviceToken(String token, String deviceModel, String osVersion, long createdAt) {
        this.token = token;
        this.deviceModel = deviceModel;
        this.osVersion = osVersion;
        this.createdAt = createdAt;
    }

    // --- getters & setters ---
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

