package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.ChangePasswordRequest;
import com.thesis.securitystudy.dto.UpdateProfileRequest;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User updateProfile(String currentUsername, UpdateProfileRequest req) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        // If username changed, check uniqueness
        if (!user.getUsername().equals(req.getUsername())) {
            if (userRepository.existsByUsername(req.getUsername())) {
                throw new IllegalArgumentException("Username already taken");
            }
            user.setUsername(req.getUsername());
        }

        // If email changed, check uniqueness
        if (!user.getEmail().equals(req.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new IllegalArgumentException("Email already registered");
            }
            user.setEmail(req.getEmail());
        }

        user.setBio(req.getBio());
        user.setAvatarUrl(req.getAvatarUrl());

        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String currentUsername, ChangePasswordRequest req) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    public Optional<User> getPublicProfile(Long id) {
        return userRepository.findById(id).map(u -> {
            User publicUser = new User();
            publicUser.setId(u.getId());
            publicUser.setUsername(u.getUsername());
            publicUser.setBio(u.getBio());
            publicUser.setAvatarUrl(u.getAvatarUrl());
            // Do not include email, password, roles, enabled
            return publicUser;
        });
    }
}

