package com.example.trouble_log.domain.projectSession.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor @AllArgsConstructor
@Schema(description = "사전 컨텍스트 내용")
public class PreContextRequest {

    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "세션 ID", example = "1")
    private Long sessionId;

    @Schema(description = "코드 의도", example = "게시판 CRUD")
    private String codePurpose;

    @Schema(description = "기술 선택 이유", example = "라이브러리, 프레임워크")
    private String techRationale;

    @Schema(description = "예외 처리", example = "과거 일자에 예약 방지")
    private String exceptionHandling;

    @Schema(description = "프로젝트 규모", example = "3주, 4인")
    private String projectScale;
}
