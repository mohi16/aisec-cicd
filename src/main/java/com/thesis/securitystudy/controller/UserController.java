package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ChangePasswordRequest;
import com.thesis.securitystudy.dto.UpdateProfileRequest;
import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;

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
        Map<String, Object> userInfo = Map.of(
                "username", auth.getName(),
                "authorities", auth.getAuthorities()
        );
        return ResponseEntity.ok(ApiResponse.ok("Current user", userInfo));
    }

    /**
     * PUT /api/users/me
     * Update profile of the currently authenticated user.
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<User>> updateProfile(Authentication auth,
                                                           @Valid @RequestBody UpdateProfileRequest request) {
        // Resolve current user by auth
        Optional<User> maybeUser = userRepository.findByUsername(auth.getName());
        if (maybeUser.isEmpty()) {
            // Noch nicht implementiert: konsistentes Fehlerhandling / ApiResponse
            throw new RuntimeException("Current user not found");
        }
        User currentUser = maybeUser.get();

        // TODO: call service to perform update and return the updated User
        // User updated = userService.updateProfile(currentUser, request);
        throw new UnsupportedOperationException("TODO: implement updateProfile endpoint");
    }

    /**
     * PUT /api/users/me/password
     * Change password for the currently authenticated user.
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(Authentication auth,
                                                            @Valid @RequestBody ChangePasswordRequest request) {
        Optional<User> maybeUser = userRepository.findByUsername(auth.getName());
        if (maybeUser.isEmpty()) {
            throw new RuntimeException("Current user not found");
        }
        User currentUser = maybeUser.get();

        // TODO: call service to change password
        // userService.changePassword(currentUser, request);
        throw new UnsupportedOperationException("TODO: implement changePassword endpoint");
    }

    /**
     * GET /api/users/{id}
     * Publicly accessible user profile (or protected depending on requirements).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        // TODO: call service to get user by id and map to a safe view (avoid exposing password)
        // User user = userService.getUserById(id);
        throw new UnsupportedOperationException("TODO: implement getUserById endpoint");
    }
}
