package com.example.trouble_log.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "최종 제출할 면접 답변")
public class InterviewSubmitAnswerRequest {

    @Schema(description = "질문 ID", example = "1")
    private Long questionId;

    @Schema(description = "최종 답변", example = "트랜잭션 경계를 서비스 계층에 두고 JPA 변경 감지를 활용했습니다.")
    private String answer;
}
