package com.example.trouble_log.domain.ai.service;

import com.example.trouble_log.domain.ai.dto.AnswerFeedbackResult;
import com.example.trouble_log.domain.ai.dto.CodeEvaluationResult;
import com.example.trouble_log.domain.ai.dto.RadarScore;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class RadarScoreCalculator {

    public RadarScore calculate(AnswerFeedbackResult feedback, CodeEvaluationResult codeEval) {
        return new RadarScore(
                toHundred5(avg(feedback.getScores().getStructure(),  feedback.getScores().getRelevance())),
                toHundred5(avg(feedback.getScores().getKeyword(),    feedback.getScores().getRelevance())),
                toHundred20(avg(codeEval.getErrorHandling().getScore(), codeEval.getSingleResponsibility().getScore())),
                toHundred5(avg(feedback.getScores().getSpecificity(), feedback.getScores().getKeyword())),
                toHundred20(avg(codeEval.getNaming().getScore(), codeEval.getDuplication().getScore(), codeEval.getCommentQuality().getScore()))
        );
    }

    private int toHundred5(double score)  { return (int) Math.round((score / 5.0)  * 100); }
    private int toHundred20(double score) { return (int) Math.round((score / 20.0) * 100); }
    private double avg(int... values) { return Arrays.stream(values).average().orElse(0); }
}
