package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.RoleUpdateRequest;
import com.thesis.securitystudy.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminService.UserAdminDto>> listUsers() {
        List<AdminService.UserAdminDto> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminService.UserAdminDto> getUser(@PathVariable Long id) {
        AdminService.UserAdminDto dto = adminService.getUserById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/users/{id}/roles")
    public ResponseEntity<AdminService.UserAdminDto> updateRoles(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {

        AdminService.UserAdminDto updated = adminService.updateUserRoles(id, request.getRoles());
        // return 200 OK with updated entity
        return ResponseEntity.ok(updated);
    }

    /**
     * Disable a user by default. To permanently delete, pass ?permanent=true
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteOrDisable(
            @PathVariable Long id,
            @RequestParam(name = "permanent", required = false, defaultValue = "false") boolean permanent) {

        adminService.deleteOrDisableUser(id, permanent);
        return ResponseEntity.noContent().build();
    }
}
