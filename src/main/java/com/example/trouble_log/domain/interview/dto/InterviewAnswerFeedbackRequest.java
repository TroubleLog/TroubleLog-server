package com.example.trouble_log.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "면접 답변 AI 피드백 요청")
public class InterviewAnswerFeedbackRequest {

    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "현재 작성 중인 사용자 답변", example = "트랜잭션 경계를 서비스 계층에 두고 JPA 변경 감지를 활용했습니다.")
    private String answer;
}
