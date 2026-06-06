package com.example.trouble_log.domain.interview.dto;

import com.example.trouble_log.domain.ai.dto.RadarScore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "트러블슈팅 리포트 및 역량 분석 응답")
public class ReportResponse {

    @Schema(description = "마크다운 형식의 트러블슈팅 리포트")
    private String report;

    @Schema(description = "역량 레이더 차트 점수")
    private RadarScore radarScore;
}
