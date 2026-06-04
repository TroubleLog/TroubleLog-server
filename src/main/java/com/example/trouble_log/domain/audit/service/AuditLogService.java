package com.example.trouble_log.domain.audit.service;

import com.example.trouble_log.domain.audit.entity.AuditLog;
import com.example.trouble_log.domain.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(
            Long memberId,
            Long sessionId,
            String action,
            String requestSummary,
            String responseSummary
    ) {
        saveSafely(memberId, sessionId, action, "SUCCESS", requestSummary, responseSummary, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordStarted(Long memberId, Long sessionId, String action, String requestSummary) {
        saveSafely(memberId, sessionId, action, "STARTED", requestSummary, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            Long memberId,
            Long sessionId,
            String action,
            String requestSummary,
            String errorMessage
    ) {
        saveSafely(memberId, sessionId, action, "FAILURE", requestSummary, null, errorMessage);
    }

    private void saveSafely(
            Long memberId,
            Long sessionId,
            String action,
            String status,
            String requestSummary,
            String responseSummary,
            String errorMessage
    ) {
        try {
            AuditLog auditLog = new AuditLog(
                    memberId,
                    sessionId,
                    action,
                    status,
                    requestSummary,
                    responseSummary,
                    errorMessage
            );
            auditLogRepository.save(auditLog);
        } catch (RuntimeException e) {
            log.warn(
                    "Failed to save audit log. memberId={}, sessionId={}, action={}, status={}, error={}",
                    memberId,
                    sessionId,
                    action,
                    status,
                    e.getMessage(),
                    e
            );
        }
    }
}
