package com.example.trouble_log.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CodeEvaluationResult {
    @JsonProperty("naming")
    private Axis naming;

    @JsonProperty("single_responsibility")   // 이미 있음, 유지
    private Axis singleResponsibility;

    @JsonProperty("error_handling")          // snake_case로 수정
    private Axis errorHandling;

    @JsonProperty("duplication")
    private Axis duplication;

    @JsonProperty("comment_quality")         // snake_case로 수정
    private Axis commentQuality;

    @Data
    public static class Axis {
        private int score;
        private String comment;
    }
}
