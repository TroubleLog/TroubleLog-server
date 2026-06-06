package com.example.trouble_log.domain.interview.controller;

import com.example.trouble_log.domain.interview.dto.AnswerRequest;
import com.example.trouble_log.domain.interview.dto.AnswerResponse;
import com.example.trouble_log.domain.interview.dto.ReportResponse;
import com.example.trouble_log.domain.interview.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
            summary = "현재 답변 AI 피드백 생성",
            description = "특정 프로젝트 세션의 면접 질문에 대해 현재 작성 중인 답변을 저장하지 않고 AI 피드백만 생성합니다. 최종 저장은 면접 답변 최종 제출 API에서 수행합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "AI 피드백 생성 성공",
                    content = @Content(schema = @Schema(implementation = AnswerResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 본문 누락"),
            @ApiResponse(responseCode = "403", description = "해당 세션의 질문이 아님"),
            @ApiResponse(responseCode = "404", description = "세션, 질문 또는 사전 컨텍스트를 찾을 수 없음")
    })
    @PostMapping("/{sessionId}/answers/{questionId}")
    public ResponseEntity<AnswerResponse> saveAnswer(
            @Parameter(description = "프로젝트 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @Parameter(description = "답변을 저장할 면접 질문 ID", example = "1", required = true)
            @PathVariable Long questionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "회원 ID와 현재 작성 중인 답변 내용. 답변이 null 또는 공백이면 AI 피드백 없이 null 값을 반환합니다.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AnswerRequest.class))
            )
            @RequestBody AnswerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewService.saveAnswer(sessionId, questionId, request));
    }

    @Operation(
            summary = "트러블슈팅 리포트 생성",
            description = "저장된 면접 질문과 답변, 사전 컨텍스트, 코드 평가 결과를 바탕으로 마크다운 리포트와 역량 레이더 점수를 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리포트 생성 성공",
                    content = @Content(schema = @Schema(implementation = ReportResponse.class))),
            @ApiResponse(responseCode = "404", description = "세션 또는 사전 컨텍스트를 찾을 수 없음")
    })
    @PostMapping("/{sessionId}/report")
    public ResponseEntity<ReportResponse> generateReport(
            @Parameter(description = "프로젝트 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(interviewService.generateReport(sessionId));
    }
}
