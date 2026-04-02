package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.model.AuditLog;
import com.thesis.securitystudy.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * GET /api/admin/audit-logs
     * Optional query params:
     * - user=USERNAME
     * - action=ACTION
     *
     * Example:
     * /api/admin/audit-logs?user=testuser
     * /api/admin/audit-logs?action=LOGIN_FAILURE
     *
     * Requires ADMIN role.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs(
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "action", required = false) String action) {

        List<AuditLog> logs;
        if (user != null && !user.isBlank()) {
            logs = auditService.findByUsername(user);
        } else if (action != null && !action.isBlank()) {
            logs = auditService.findByAction(action);
        } else {
            logs = auditService.findAll();
        }

        return ResponseEntity.ok(ApiResponse.ok("Audit logs fetched", logs));
    }
}
