package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.AuditLog;
import com.thesis.securitystudy.repository.AuditLogRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Generic record method for audit events.
     */
    public AuditLog record(String action,
                           String entityType,
                           Long entityId,
                           String username,
                           String details,
                           String ipAddress,
                           AuditLog.AuditLevel level) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setUsername(username);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setLevel(Objects.requireNonNullElse(level, AuditLog.AuditLevel.INFO));
        return auditLogRepository.save(log);
    }

    /**
     * Convenience overloads
     */
    public AuditLog record(String action,
                           String entityType,
                           Long entityId,
                           String username,
                           String ipAddress) {
        return record(action, entityType, entityId, username, null, ipAddress, AuditLog.AuditLevel.INFO);
    }

    public AuditLog record(String action,
                           String entityType,
                           String username,
                           String ipAddress) {
        return record(action, entityType, null, username, null, ipAddress, AuditLog.AuditLevel.INFO);
    }

    /* Query helpers used by the admin controller */
    public List<AuditLog> findAll() {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    public List<AuditLog> findByUsername(String username) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username);
    }

    public List<AuditLog> findByAction(String action) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action);
    }
}
