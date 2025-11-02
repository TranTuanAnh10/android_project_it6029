package vn.haui.android_project.entity;

public class UserLocationEntity {
    private String id;
    private double latitude;
    private double longitude;
    private String address;
    private String phoneNumber;
    private boolean defaultLocation;

    private String locationType;

    public UserLocationEntity() {}

    public UserLocationEntity(String id, double latitude, double longitude, String address, String phoneNumber, boolean defaultLocation, String locationType) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.phoneNumber= phoneNumber;
        this.defaultLocation = defaultLocation;
        this.locationType= locationType;
    }

    public boolean isDefaultLocation() {
        return defaultLocation;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean getDefault() { return defaultLocation; }
    public void setDefaultLocation(boolean defaultLocation) { this.defaultLocation = defaultLocation; }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}

