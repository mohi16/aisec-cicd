package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.RoleUpdateRequest;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<User> listAllUsers() {
        return adminService.listUsers();
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return adminService.getUser(id);
    }

    @PutMapping("/{id}/roles")
    public User updateRoles(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        return adminService.updateRoles(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
