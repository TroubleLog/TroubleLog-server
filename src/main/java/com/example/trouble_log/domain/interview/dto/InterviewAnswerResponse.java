package com.example.trouble_log.domain.interview.dto;

import com.example.trouble_log.domain.ai.dto.AnswerFeedbackResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "면접 답변 저장 응답")
public class InterviewAnswerResponse {

    @Schema(description = "질문 ID", example = "1")
    private Long questionId;

    @Schema(description = "질문 순서", example = "1")
    private Integer questionSequence;

    @Schema(description = "면접 질문", example = "Spring Boot와 JPA를 선택한 이유는 무엇인가요?")
    private String question;

    @Schema(description = "저장된 답변", example = "트랜잭션 관리와 생산성 측면에서 선택했습니다.")
    private String answer;

    @Schema(description = "답변 건너뛰기 여부", example = "false")
    private Boolean isSkipped;

    @Schema(description = "AI 피드백 결과")
    private AnswerFeedbackResult feedback;
}
