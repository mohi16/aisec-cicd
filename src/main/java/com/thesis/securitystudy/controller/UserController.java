package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.dto.ChangePasswordRequest;
import com.thesis.securitystudy.dto.UpdateProfileRequest;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(Authentication auth) {
        User currentUser = resolveCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.ok("Current user", toOwnProfileView(currentUser)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(Authentication auth,
                                                                          @Valid @RequestBody UpdateProfileRequest request) {
        User currentUser = resolveCurrentUser(auth);
        User updated = userService.updateProfile(currentUser, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", toOwnProfileView(updated)));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(Authentication auth,
                                                            @Valid @RequestBody ChangePasswordRequest request) {
        User currentUser = resolveCurrentUser(auth);
        userService.changePassword(currentUser, request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok("User profile", toPublicProfileView(user)));
    }

    private User resolveCurrentUser(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user not found"));
    }

    private Map<String, Object> toOwnProfileView(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("roles", user.getRoles());
        data.put("enabled", user.isEnabled());
        data.put("bio", user.getBio());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("createdAt", user.getCreatedAt());
        return data;
    }

    private Map<String, Object> toPublicProfileView(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("bio", user.getBio());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("createdAt", user.getCreatedAt());
        return data;
    }
}