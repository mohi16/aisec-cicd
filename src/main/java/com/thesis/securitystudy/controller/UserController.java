package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.ChangePasswordRequest;
import com.thesis.securitystudy.dto.UpdateProfileRequest;
import com.thesis.securitystudy.dto.UserResponse;
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

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Return current authenticated user's profile
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication auth) {
        User user = userService.getByUsername(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Current user", UserResponse.from(user)));
    }

    // Update profile (partial updates allowed)
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request) {

        User updated = userService.updateProfile(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", UserResponse.from(updated)));
    }

    // Change password
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated"));
    }

    // Get user profile by id (public for now - controller security config determines access)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("User profile", UserResponse.from(user)));
    }
}
