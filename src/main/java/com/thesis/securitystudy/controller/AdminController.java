package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.RoleUpdateRequest;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
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
    public ResponseEntity<List<User>> listUsers() {
        // TODO: Implement listing of users via adminService.listUsers()
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/admin/users/{id}
     * Get details for a single user
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") Long id) {
        // TODO: Implement fetching a single user via adminService.getUserById(id)
        return ResponseEntity.notFound().build();
    }

    /**
     * PUT /api/admin/users/{id}/roles
     * Update roles for a user
     */
    @PutMapping("/users/{id}/roles")
    public ResponseEntity<User> updateRoles(
            @PathVariable("id") Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        // TODO: Convert request.roles (Set<String>) to Set<Role> and call adminService.updateRoles(id, roles)
        return ResponseEntity.badRequest().build();
    }

    /**
     * DELETE /api/admin/users/{id}
     * Disable or delete a user (implementation detail up to service)
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteOrDisableUser(@PathVariable("id") Long id) {
        // TODO: Call adminService.deleteOrDisableUser(id)
        return ResponseEntity.notFound().build();
    }
}
