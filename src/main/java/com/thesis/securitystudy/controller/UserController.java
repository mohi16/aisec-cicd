package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.ChangePasswordRequest;
import com.thesis.securitystudy.dto.UpdateProfileRequest;
import com.thesis.securitystudy.dto.UserResponse;
import com.thesis.securitystudy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication auth) {
        UserResponse response = userService.getCurrentUserProfile(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Current user", response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse response = userService.updateProfile(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", response));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@PathVariable Long id) {
        UserResponse response = userService.getPublicProfile(id);
        return ResponseEntity.ok(ApiResponse.ok("User profile", response));
    }
}