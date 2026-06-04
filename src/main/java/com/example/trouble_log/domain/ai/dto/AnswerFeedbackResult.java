package com.example.trouble_log.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AnswerFeedbackResult {
    private Scores scores;

    @JsonProperty("improvement")   
    private String improvement;

    private String warning;

    @Data
    public static class Scores {
        private int specificity;
        private int structure;
        private int relevance;
        private int keyword;
    }
}
