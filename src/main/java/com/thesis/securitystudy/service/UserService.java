package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.ChangePasswordRequest;
import com.thesis.securitystudy.dto.UpdateProfileRequest;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    // ggf. später: PasswordEncoder, AuditService, etc.

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Update user's profile fields (username, email, bio, avatarUrl).
     * TODO: implement validation, uniqueness checks, persist changes.
     */
    public User updateProfile(User currentUser, UpdateProfileRequest request) {
        // TODO: implement profile update logic (check username/email uniqueness, validation, save)
        throw new UnsupportedOperationException("TODO: implement updateProfile");
    }

    /**
     * Change the user's password after verifying currentPassword.
     * TODO: implement verification using PasswordEncoder and save new password.
     */
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        // TODO: implement password change (verify current password, encode new password, save)
        throw new UnsupportedOperationException("TODO: implement changePassword");
    }

    /**
     * Retrieve a user by id.
     * TODO: implement retrieval and handling for not found.
     */
    public User getUserById(Long id) {
        // TODO: implement retrieval (e.g., userRepository.findById(id).orElseThrow(...))
        throw new UnsupportedOperationException("TODO: implement getUserById");
    }
}
