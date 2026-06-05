package com.example.trouble_log.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "답변 제출 요청")
public class AnswerRequest {

    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "답변 내용 (null 또는 빈 값이면 스킵 처리)", example = "루프를 분리한 이유는...")
    private String answer;
}
