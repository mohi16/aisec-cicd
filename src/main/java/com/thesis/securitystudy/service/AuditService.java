package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.AuditLog;
import com.thesis.securitystudy.repository.AuditLogRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Record an audit event with full details.
     * Any caller (other services/controllers) can use this to persist an audit entry.
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
        if (level != null) {
            log.setLevel(level);
        }
        return auditLogRepository.save(log);
    }

    /**
     * Convenience: accept a ready AuditLog instance.
     */
    public AuditLog record(AuditLog auditLog) {
        Objects.requireNonNull(auditLog, "auditLog must not be null");
        return auditLogRepository.save(auditLog);
    }

    /**
     * List all audit logs ordered by timestamp descending.
     */
    public List<AuditLog> listAll() {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    /**
     * Find logs for a specific username ordered by timestamp desc.
     */
    public List<AuditLog> findByUsername(String username) {
        if (username == null) return List.of();
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username);
    }

    /**
     * Find logs with a specific action ordered by timestamp desc.
     */
    public List<AuditLog> findByAction(String action) {
        if (action == null) return List.of();
        return auditLogRepository.findByActionOrderByTimestampDesc(action);
    }

    /**
     * Combined / flexible filter: when callers provide both user and action,
     * this implementation performs a composed filter in-memory (keeps repository API small).
     */
    public List<AuditLog> findByUsernameAndAction(String username, String action) {
        if (username == null && action == null) {
            return listAll();
        }
        if (username != null && action == null) {
            return findByUsername(username);
        }
        if (username == null) {
            return findByAction(action);
        }
        // both non-null: fetch by username (more selective) then filter by action
        return findByUsername(username).stream()
                .filter(l -> action.equals(l.getAction()))
                .collect(Collectors.toList());
    }
}
