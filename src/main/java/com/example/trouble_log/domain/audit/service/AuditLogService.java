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

    // 요청이 성공적으로 끝난 경우의 감사 로그를 별도 트랜잭션으로 저장한다.
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

    // 요청 처리가 시작되었음을 나타내는 감사 로그를 별도 트랜잭션으로 저장한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordStarted(Long memberId, Long sessionId, String action, String requestSummary) {
        saveSafely(memberId, sessionId, action, "STARTED", requestSummary, null, null);
    }

    // 요청 처리 실패 정보를 감사 로그로 별도 트랜잭션에 저장한다.
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

    // 감사 로그 저장 중 예외가 발생해도 본 요청 흐름은 계속 진행되도록 안전하게 저장한다.
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
