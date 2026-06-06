package com.example.trouble_log.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "면접 답변 최종 제출 응답")
public class InterviewSubmitResponse {

    @Schema(description = "제출 완료 여부", example = "true")
    private Boolean submitted;

    @Schema(description = "리포트 생성 진행 가능 여부", example = "true")
    private Boolean reportGenerationReady;

    @Schema(description = "차단 사유", example = "PERSONAL_INFO_DETECTED")
    private String blockedReason;

    @Schema(description = "개인정보 감지 경고 목록")
    private List<PersonalInfoWarning> warnings;
}
