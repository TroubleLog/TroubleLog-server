package com.example.trouble_log.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "답변 제출 응답")
public class AnswerResponse {

    @Schema(description = "저장된 답변 ID")
    private Long answerId;

    @Schema(description = "AI 피드백 개선 제안 (스킵 시 null)")
    private String improvement;

    @Schema(description = "주의 메시지 (없으면 null)")
    private String warning;
}
