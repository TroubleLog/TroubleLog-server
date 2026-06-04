package com.example.trouble_log.domain.audit.service;

import com.example.trouble_log.domain.audit.entity.AuditLog;
import com.example.trouble_log.domain.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
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
        } catch (RuntimeException ignored) {
            // 감사 로그 저장 실패가 사용자 기능 실패로 이어지지 않도록 삼킨다.
        }
    }
}
