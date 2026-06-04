package com.example.trouble_log.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RadarScore {
    private int problemSolving;    // 문제해결력
    private int techJudgment;      // 기술 판단력
    private int codeReliability;   // 코드 신뢰성
    private int communication;     // 커뮤니케이션
    private int designThinking;    // 설계 사고력
}
