package com.example.trouble_log.domain.audit.dto;

import com.example.trouble_log.domain.audit.entity.AuditLog;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "감사 로그 응답")
public class AuditLogResponse {

    @Schema(description = "감사 로그 ID", example = "1")
    private Long id;

    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "프로젝트 세션 ID", example = "10")
    private Long sessionId;

    @Schema(description = "행위 이름", example = "AI_INTERVIEW_QUESTION_GENERATE")
    private String action;

    @Schema(description = "처리 상태", example = "SUCCESS")
    private String status;

    @Schema(description = "요청 요약")
    private String requestSummary;

    @Schema(description = "응답 요약")
    private String responseSummary;

    @Schema(description = "실패 메시지")
    private String errorMessage;

    @Schema(description = "로그 생성 시각")
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getMemberId(),
                auditLog.getSessionId(),
                auditLog.getAction(),
                auditLog.getStatus(),
                auditLog.getRequestSummary(),
                auditLog.getResponseSummary(),
                auditLog.getErrorMessage(),
                auditLog.getCreatedAt()
        );
    }
}
