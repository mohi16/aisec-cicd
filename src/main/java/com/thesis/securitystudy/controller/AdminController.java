package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.RoleUpdateRequest;
import com.thesis.securitystudy.model.Role;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/users")
    public ResponseEntity<List<UserAdminResponse>> listUsers() {
        List<UserAdminResponse> users = adminService.getAllUsers()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserAdminResponse> getUser(@PathVariable Long id) {
        User user = adminService.getUserById(id);
        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping("/users/{id}/roles")
    public ResponseEntity<UserAdminResponse> updateRoles(@PathVariable Long id,
                                                         @Valid @RequestBody RoleUpdateRequest request) {
        User updated = adminService.updateUserRoles(id, request.getRoles());
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteOrDisable(@PathVariable Long id,
                                                @RequestParam(defaultValue = "false") boolean permanent) {
        adminService.deleteOrDisableUser(id, permanent);
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