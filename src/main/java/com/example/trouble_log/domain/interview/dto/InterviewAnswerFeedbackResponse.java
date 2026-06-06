package com.example.trouble_log.domain.interview.dto;

import com.example.trouble_log.domain.ai.dto.AnswerFeedbackResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "면접 답변 AI 피드백 응답")
public class InterviewAnswerFeedbackResponse {

    @Schema(description = "질문 ID", example = "1")
    private Long questionId;

    @Schema(description = "질문 순서", example = "1")
    private Integer questionSequence;

    @Schema(description = "AI 피드백 결과")
    private AnswerFeedbackResult feedback;
}
