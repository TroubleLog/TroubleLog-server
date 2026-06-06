package com.example.trouble_log.domain.interview.controller;

import com.example.trouble_log.domain.interview.dto.InterviewSubmitRequest;
import com.example.trouble_log.domain.interview.dto.InterviewSubmitResponse;
import com.example.trouble_log.domain.interview.service.InterviewSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
@Tag(name = "Interview Submission", description = "면접 답변 최종 제출 API")
public class InterviewSubmissionController {

    private final InterviewSubmissionService interviewSubmissionService;

    @Operation(
            summary = "면접 답변 최종 제출",
            description = "프로젝트 세션에 생성된 면접 질문 3개에 대한 최종 답변을 저장합니다. 개인정보와 민감정보 감지는 최초 소스코드 입력 단계에서 수행합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "최종 제출 성공",
                    content = @Content(schema = @Schema(implementation = InterviewSubmitResponse.class))),
            @ApiResponse(responseCode = "400", description = "필수 값 누락, 질문 3개 미준비, 답변 개수 불일치"),
            @ApiResponse(responseCode = "403", description = "해당 프로젝트 세션에 접근할 수 없음"),
            @ApiResponse(responseCode = "404", description = "프로젝트 세션을 찾을 수 없음")
    })
    @PostMapping("/{sessionId}/interview/submit")
    public ResponseEntity<InterviewSubmitResponse> submit(
            @Parameter(description = "프로젝트 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "회원 ID와 질문별 최종 답변 3개",
                    required = true,
                    content = @Content(schema = @Schema(implementation = InterviewSubmitRequest.class))
            )
            @RequestBody InterviewSubmitRequest request
    ) {
        InterviewSubmitResponse response = interviewSubmissionService.submit(sessionId, request);
        return ResponseEntity.ok(response);
    }
}
