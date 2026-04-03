package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.RoleUpdateRequest;
import com.thesis.securitystudy.model.Role;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * GET /api/admin/users
     * List all users
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserAdminResponse>> listUsers() {
        List<UserAdminResponse> users = adminService.listUsers()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/admin/users/{id}
     * Get details for a single user
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserAdminResponse> getUserById(@PathVariable("id") Long id) {
        User user = adminService.getUserById(id);
        return ResponseEntity.ok(toResponse(user));
    }

    /**
     * PUT /api/admin/users/{id}/roles
     * Update roles for a user
     */
    @PutMapping("/users/{id}/roles")
    public ResponseEntity<UserAdminResponse> updateRoles(
            @PathVariable("id") Long id,
            @Valid @RequestBody RoleUpdateRequest request) {

        Set<Role> roles = EnumSet.noneOf(Role.class);

        for (String value : request.getRoles()) {
            if (value == null || value.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must not be blank");
            }

            try {
                roles.add(Role.valueOf(value.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + value);
            }
        }

        User updated = adminService.updateRoles(id, roles);
        return ResponseEntity.ok(toResponse(updated));
    }

    /**
     * DELETE /api/admin/users/{id}
     * Disable a user
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteOrDisableUser(@PathVariable("id") Long id) {
        adminService.deleteOrDisableUser(id);
        return ResponseEntity.noContent().build();
    }

    private UserAdminResponse toResponse(User user) {
        return new UserAdminResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.isEnabled()
        );
    }

    public record UserAdminResponse(
            Long id,
            String username,
            String email,
            Set<Role> roles,
            boolean enabled
    ) {
    }
}