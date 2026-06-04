package com.example.trouble_log.domain.audit.controller;

import com.example.trouble_log.domain.audit.dto.AuditLogResponse;
import com.example.trouble_log.domain.audit.service.AuditLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    @Operation(summary = "감사 로그 조회", description = "감사 로그를 최신순으로 조회합니다. action, status, memberId, sessionId로 필터링할 수 있습니다.")
    public ResponseEntity<List<AuditLogResponse>> findAuditLogs(
            @Parameter(description = "행위 이름", example = "AI_INTERVIEW_QUESTION_GENERATE")
            @RequestParam(required = false) String action,
            @Parameter(description = "처리 상태", example = "FAILURE")
            @RequestParam(required = false) String status,
            @Parameter(description = "회원 ID", example = "1")
            @RequestParam(required = false) Long memberId,
            @Parameter(description = "프로젝트 세션 ID", example = "10")
            @RequestParam(required = false) Long sessionId
    ) {
        List<AuditLogResponse> response = auditLogQueryService.findAuditLogs(action, status, memberId, sessionId);
        return ResponseEntity.ok(response);
    }
}
