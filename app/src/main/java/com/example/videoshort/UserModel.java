package com.example.videoshort;

import java.io.Serializable;

public class UserModel implements Serializable {
    private String userId;
    private String username;
    private String email;

    // Constructor mặc định
    public UserModel() {
    }

    // Constructor với tham số
    public UserModel(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    // Getter và Setter
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}