package com.example.trouble_log.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "면접 답변 최종 제출 요청")
public class InterviewSubmitRequest {

    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "최종 제출할 면접 답변 목록")
    private List<InterviewSubmitAnswerRequest> answers;
}
