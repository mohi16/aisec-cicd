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
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest req) {

        User updated = userService.updateProfile(auth.getName(), req);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", UserResponse.from(updated)));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest req) {

        userService.changePassword(auth.getName(), req);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getPublicProfile(@PathVariable Long id) {
        return userService.getPublicProfile(id)
                .map(u -> ResponseEntity.ok(ApiResponse.ok("Public profile", UserResponse.from(u))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
