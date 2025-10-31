package vn.haui.android_project.entity;

public class UserLocationEntity {
    private String id;
    private double latitude;
    private double longitude;
    private String address;
    private boolean type;

    public UserLocationEntity() {}

    public UserLocationEntity(String id, double latitude, double longitude, String address, boolean type) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.type = type;
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

    public boolean getType() { return type; }
    public void setType(boolean type) { this.type = type; }
}

