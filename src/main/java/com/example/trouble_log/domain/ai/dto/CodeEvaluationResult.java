package com.example.trouble_log.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CodeEvaluationResult {
    @JsonProperty("naming")
    private Axis naming;
    @JsonProperty("single_responsibility")
    private Axis singleResponsibility;
    @JsonProperty("error_handling")
    private Axis errorHandling;
    @JsonProperty("duplication")
    private Axis duplication;
    @JsonProperty("comment_quality")
    private Axis commentQuality;
    private int total;

    @Data
    public static class Axis {
        private int score;
        private String comment;
    }
}
