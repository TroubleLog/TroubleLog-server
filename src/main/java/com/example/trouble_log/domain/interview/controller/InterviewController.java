package com.example.trouble_log.domain.interview.controller;

import com.example.trouble_log.domain.interview.dto.AnswerRequest;
import com.example.trouble_log.domain.interview.dto.AnswerResponse;
import com.example.trouble_log.domain.interview.dto.ReportResponse;
import com.example.trouble_log.domain.interview.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
@Tag(name = "Interview", description = "면접 답변 및 리포트 API")
public class InterviewController {

    private final InterviewService interviewService;

    @Operation(summary = "답변 제출", description = "면접 질문에 대한 답변을 저장하고 AI 피드백을 반환합니다.")
    @PostMapping("/{sessionId}/answers/{questionId}")
    public ResponseEntity<AnswerResponse> saveAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @RequestBody AnswerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewService.saveAnswer(sessionId, questionId, request));
    }

    @Operation(summary = "리포트 생성", description = "저장된 Q&A를 바탕으로 트러블슈팅 리포트를 생성합니다.")
    @PostMapping("/{sessionId}/report")
    public ResponseEntity<ReportResponse> generateReport(
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(interviewService.generateReport(sessionId));
    }
}
