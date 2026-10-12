package com.medicareai.service;

import com.medicareai.entity.AuditLog;
import com.medicareai.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(Long actorUserId, String action, String entityType, Long entityId, String description) {
        AuditLog log = AuditLog.builder()
                .actorUserId(actorUserId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build();
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }
}
