package vn.haui.android_project.entity;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class UserEntity {
    private String uid;
    private String name;
    private String email;
    private String avatarUrl;
    private String role;
    private String createdAt;
    private String lastLogin;
    private String phoneNumber;
    private List<DeviceToken> tokens;
    public UserEntity() {
    }

    public UserEntity(String uid, String name, String email, String avatarUrl, String role, String createdAt, String lastLogin, String phoneNumber) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    public List<DeviceToken> getTokens() {
        return tokens;
    }

    public void setTokens(List<DeviceToken> tokens) {
        this.tokens = tokens;
    }

    public void addToken(DeviceToken token) {
        if (this.tokens == null) this.tokens = new ArrayList<>();
        this.tokens.add(token);
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }
}
