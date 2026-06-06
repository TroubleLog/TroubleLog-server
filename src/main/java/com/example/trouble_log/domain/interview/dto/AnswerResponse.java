package com.example.trouble_log.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "현재 답변 AI 피드백 응답")
public class AnswerResponse {

    @Schema(description = "저장된 답변 ID. 현재 답변 피드백 API에서는 DB에 저장하지 않으므로 null입니다.", nullable = true)
    private Long answerId;

    @Schema(description = "AI 피드백 개선 제안 (스킵 시 null)")
    private String improvement;

    @Schema(description = "주의 메시지 (없으면 null)")
    private String warning;
}
