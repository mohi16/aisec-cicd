package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.ChangePasswordRequest;
import com.thesis.securitystudy.dto.UpdateProfileRequest;
import com.thesis.securitystudy.dto.UserResponse;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getCurrentUserProfile(String username) {
        User user = findByUsername(username);
        return UserResponse.from(user);
    }

    public UserResponse updateProfile(String currentUsername, UpdateProfileRequest request) {
        User user = findByUsername(currentUsername);

        String newUsername = request.getUsername().trim();
        String newEmail = request.getEmail().trim().toLowerCase();

        if (!user.getUsername().equals(newUsername) && userRepository.existsByUsername(newUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken");
        }

        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        user.setUsername(newUsername);
        user.setEmail(newEmail);
        user.setBio(normalizeOptionalField(request.getBio()));
        user.setAvatarUrl(normalizeOptionalField(request.getAvatarUrl()));

        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    public void changePassword(String currentUsername, ChangePasswordRequest request) {
        User user = findByUsername(currentUsername);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public UserResponse getPublicProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return UserResponse.fromPublic(user);
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeOptionalField(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}