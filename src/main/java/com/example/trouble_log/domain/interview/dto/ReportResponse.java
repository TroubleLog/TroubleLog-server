package com.example.trouble_log.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "트러블슈팅 리포트 응답")
public class ReportResponse {

    @Schema(description = "마크다운 형식의 트러블슈팅 리포트")
    private String report;
}
