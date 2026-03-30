package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.ChangePasswordRequest;
import com.thesis.securitystudy.dto.UpdateProfileRequest;
import com.thesis.securitystudy.exception.ResourceNotFoundException;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> updateOwnProfile(String currentUsername, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newUsername = request.getUsername().trim();
        String newEmail = request.getEmail().trim();

        userRepository.findByUsername(newUsername)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Username already taken");
                });

        userRepository.findByEmail(newEmail)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email already registered");
                });

        user.setUsername(newUsername);
        user.setEmail(newEmail);
        user.setBio(normalizeNullable(request.getBio()));
        user.setAvatarUrl(normalizeNullable(request.getAvatarUrl()));

        User savedUser = userRepository.save(user);
        return toOwnProfile(savedUser);
    }

    public void changeOwnPassword(String currentUsername, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public Map<String, Object> getPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("bio", user.getBio());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("createdAt", user.getCreatedAt());
        return profile;
    }

    private Map<String, Object> toOwnProfile(User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("bio", user.getBio());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("roles", user.getRoles());
        profile.put("enabled", user.isEnabled());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("updatedAt", user.getUpdatedAt());
        return profile;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
