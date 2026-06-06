package com.example.trouble_log.domain.interview.dto;

import com.example.trouble_log.domain.interview.type.PersonalInfoType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "개인정보 감지 경고")
public class PersonalInfoWarning {

    @Schema(description = "감지된 개인정보 유형", example = "EMAIL")
    private PersonalInfoType type;

    @Schema(description = "프론트 표시용 경고 메시지", example = "이메일 주소로 보이는 정보가 포함되어 있습니다.")
    private String message;
}
