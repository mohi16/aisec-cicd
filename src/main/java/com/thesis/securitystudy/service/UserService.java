package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.ChangePasswordRequest;
import com.thesis.securitystudy.dto.UpdateProfileRequest;
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

    public User updateProfile(User currentUser, UpdateProfileRequest request) {
        String newUsername = request.getUsername().trim();
        String newEmail = request.getEmail().trim().toLowerCase();

        if (!newUsername.equals(currentUser.getUsername())) {
            userRepository.findByUsername(newUsername).ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken");
            });
        }

        if (!newEmail.equalsIgnoreCase(currentUser.getEmail())) {
            userRepository.findByEmail(newEmail).ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
            });
        }

        currentUser.setUsername(newUsername);
        currentUser.setEmail(newEmail);
        currentUser.setBio(normalizeOptionalText(request.getBio()));
        currentUser.setAvatarUrl(normalizeOptionalText(request.getAvatarUrl()));

        return userRepository.save(currentUser);
    }

    public void changePassword(User currentUser, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}