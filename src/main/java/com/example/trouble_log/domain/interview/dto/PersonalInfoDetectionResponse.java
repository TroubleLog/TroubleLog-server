package com.example.trouble_log.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "개인정보 감지 차단 응답")
public class PersonalInfoDetectionResponse {

    @Schema(description = "차단 사유", example = "PERSONAL_INFO_DETECTED")
    private String blockedReason;

    @Schema(description = "개인정보 감지 경고 목록")
    private List<PersonalInfoWarning> warnings;
}
