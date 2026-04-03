package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.AuditLog;
import com.thesis.securitystudy.repository.AuditLogRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

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
        log.setLevel(level != null ? level : AuditLog.AuditLevel.INFO);

        return auditLogRepository.save(log);
    }

    public List<AuditLog> findLogs(String username, String action) {
        if (username != null && !username.isBlank()) {
            List<AuditLog> logs = auditLogRepository.findByUsernameOrderByTimestampDesc(username);

            if (action != null && !action.isBlank()) {
                return logs.stream()
                        .filter(log -> action.equals(log.getAction()))
                        .toList();
            }

            return logs;
        }

        if (action != null && !action.isBlank()) {
            return auditLogRepository.findByActionOrderByTimestampDesc(action);
        }

        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }

}