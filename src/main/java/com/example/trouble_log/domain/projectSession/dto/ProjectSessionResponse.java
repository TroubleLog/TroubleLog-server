package com.example.trouble_log.domain.projectSession.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor @AllArgsConstructor
@Schema(description = "프로젝트 세션 생성 응답")
public class ProjectSessionResponse {
    @Schema(description = "세션 ID", example = "1")
    private Long sessionId;
}
