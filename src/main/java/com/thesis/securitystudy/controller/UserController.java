package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.ChangePasswordRequest;
import com.thesis.securitystudy.dto.UpdateProfileRequest;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }

    // Response DTO to avoid exposing password
    public static class ProfileResponse {

        public Long id;
        public String username;
        public String email;
        public String bio;
        public String avatarUrl;

        public ProfileResponse(
                Long id,
                String username,
                String email,
                String bio,
                String avatarUrl
        ) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.bio = bio;
            this.avatarUrl = avatarUrl;
        }
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>>
    updateProfile(
            @Valid
            @RequestBody
            UpdateProfileRequest request,

            Authentication authentication
    ) {

        String currentUsername =
                authentication.getName();

        User updated =
                userService.updateProfile(
                        currentUsername,
                        request
                );

        ProfileResponse resp =
                new ProfileResponse(
                        updated.getId(),
                        updated.getUsername(),
                        updated.getEmail(),
                        updated.getBio(),
                        updated.getAvatarUrl()
                );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Profile updated",
                        resp
                )
        );
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<String>>
    changePassword(
            @Valid
            @RequestBody
            ChangePasswordRequest request,

            Authentication authentication
    ) {

        String currentUsername =
                authentication.getName();

        userService.changePassword(
                currentUsername,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Password changed successfully"
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProfileResponse>>
    getPublicProfile(
            @PathVariable Long id
    ) {

        User user =
                userService.getById(id);

        // public profile does not include email
        ProfileResponse resp =
                new ProfileResponse(
                        user.getId(),
                        user.getUsername(),
                        null,
                        user.getBio(),
                        user.getAvatarUrl()
                );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "User profile",
                        resp
                )
        );
    }
}