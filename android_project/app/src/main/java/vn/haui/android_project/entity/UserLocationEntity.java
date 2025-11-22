package vn.haui.android_project.entity;
import com.google.firebase.database.PropertyName;

import java.io.Serializable;
import java.util.Map;

public class UserLocationEntity implements Serializable {
    private String id;
    private double latitude;
    private double longitude;
    private String address;
    private String phoneNumber;
    private boolean defaultLocation;

    private String locationType;
    private String recipientName;
    private String country;
    private String zipCode;

    private boolean isDefault;

    public UserLocationEntity() {}

    public UserLocationEntity(String id,String recipientName, double latitude, double longitude, String address,
                              String phoneNumber, boolean defaultLocation, String locationType,
                              String country, String zipCode) {
        this.id = id;
        this.recipientName= recipientName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.phoneNumber= phoneNumber;
        this.defaultLocation = defaultLocation;
        this.locationType= locationType;
        this.country=country;
        this.zipCode=zipCode;
    }


    @PropertyName("default")
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
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

    @PropertyName("default")
    public boolean getDefault() { return defaultLocation; }
    public void setDefaultLocation(boolean defaultLocation) { this.defaultLocation = defaultLocation; }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }


    public static UserLocationEntity fromMap(Map<String, Object> map) {
        UserLocationEntity entity = new UserLocationEntity();
        if (map == null) return entity;
        entity.setId((String) map.get("id"));
        entity.setAddress((String) map.get("address"));
        entity.setPhoneNumber((String) map.get("phoneNumber"));
        entity.setLocationType((String) map.get("locationType"));
        entity.setRecipientName((String) map.get("recipientName"));
        entity.setCountry((String) map.get("country"));
        entity.setZipCode((String) map.get("zipCode"));
        // Xử lý Boolean
        Boolean defaultLoc = (Boolean) map.get("defaultLocation");
        entity.setDefaultLocation(defaultLoc != null ? defaultLoc : false);
        // Xử lý Double/Long (Latitude/Longitude)
        Object latObj = map.get("latitude");
        if (latObj instanceof Number) {
            entity.setLatitude(((Number) latObj).doubleValue());
        }
        Object lonObj = map.get("longitude");
        if (lonObj instanceof Number) {
            entity.setLongitude(((Number) lonObj).doubleValue());
        }

        return entity;
    }
}

