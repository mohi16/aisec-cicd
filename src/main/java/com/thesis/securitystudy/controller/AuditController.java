package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import com.thesis.securitystudy.model.AuditLog;
import com.thesis.securitystudy.service.AuditService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * GET /api/admin/audit-logs
     * Optional query params:
     *   - user=<username>
     *   - action=<action>
     *
     * If both are provided, both filters are applied (username first, then action).
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs(
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "action", required = false) String action) {

        List<AuditLog> logs = auditService.findByUsernameAndAction(user, action);
        return ResponseEntity.ok(ApiResponse.ok("Audit logs", logs));
    }
}
