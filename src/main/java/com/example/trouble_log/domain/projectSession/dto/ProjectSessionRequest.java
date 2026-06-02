package com.example.trouble_log.domain.projectSession.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor @AllArgsConstructor
@Schema(description = "프로젝트 세션 생성 요청")
public class ProjectSessionRequest {
    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "사용자가 직접 붙여넣은 소스코드", example = "public class Main { }")
    private String codeContent;

    @Schema(description = "GitHub 저장소 URL", example = "https://github.com/user/project")
    private String githubUrl;
}
