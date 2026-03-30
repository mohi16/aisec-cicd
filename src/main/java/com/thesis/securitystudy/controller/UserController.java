package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.ChangePasswordRequest;
import com.thesis.securitystudy.dto.UpdateProfileRequest;
import com.thesis.securitystudy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(Authentication auth) {
        Map<String, Object> userInfo = Map.of(
                "username", auth.getName(),
                "authorities", auth.getAuthorities()
        );
        return ResponseEntity.ok(ApiResponse.ok("Current user", userInfo));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOwnProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request) {
        Map<String, Object> updatedUser = userService.updateOwnProfile(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updatedUser));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<String>> changeOwnPassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPublicProfile(@PathVariable Long id) {
        Map<String, Object> publicProfile = userService.getPublicProfile(id);
        return ResponseEntity.ok(ApiResponse.ok("Public profile", publicProfile));
    }
}
