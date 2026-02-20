package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.ChangePasswordRequest;
import com.thesis.securitystudy.dto.UpdateProfileRequest;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User updateProfile(
            String currentUsername,
            UpdateProfileRequest req
    ) {

        User user = userRepository
                .findByUsername(currentUsername)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        if (
                req.getUsername() != null &&
                        !req.getUsername().equals(user.getUsername())
        ) {
            if (
                    userRepository.existsByUsername(
                            req.getUsername()
                    )
            ) {
                throw new IllegalArgumentException(
                        "Username already taken"
                );
            }

            user.setUsername(req.getUsername());
        }

        if (
                req.getEmail() != null &&
                        !req.getEmail().equals(user.getEmail())
        ) {
            if (
                    userRepository.existsByEmail(
                            req.getEmail()
                    )
            ) {
                throw new IllegalArgumentException(
                        "Email already registered"
                );
            }

            user.setEmail(req.getEmail());
        }

        if (req.getBio() != null) {
            user.setBio(req.getBio());
        }

        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
        }

        return userRepository.save(user);
    }

    public void changePassword(
            String currentUsername,
            ChangePasswordRequest req
    ) {

        User user = userRepository
                .findByUsername(currentUsername)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        if (
                !passwordEncoder.matches(
                        req.getCurrentPassword(),
                        user.getPassword()
                )
        ) {
            throw new IllegalArgumentException(
                    "Current password is incorrect"
            );
        }

        user.setPassword(
                passwordEncoder.encode(
                        req.getNewPassword()
                )
        );

        userRepository.save(user);
    }

    public User getById(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
    }
}