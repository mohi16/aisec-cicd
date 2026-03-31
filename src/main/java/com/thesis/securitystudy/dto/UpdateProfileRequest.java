package com.thesis.securitystudy.dto;

public class UpdateProfileRequest {

    private String username;
    private String email;
    private String bio;
    private String avatarUrl;

    public String getUsername() { return username; }

    public String getEmail() { return email; }

    public String getBio() { return bio; }

    public String getAvatarUrl() { return avatarUrl; }
}
