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
                // 문제해결력: 트러블슈팅 답변의 구조성 + 연관성
                toHundred5(avg(
                        feedback.getScores().getStructure(),
                        feedback.getScores().getRelevance())),

                // 기술 판단력: 기술 키워드 사용 + 구체적 근거 제시
                toHundred5(avg(
                        feedback.getScores().getKeyword(),
                        feedback.getScores().getSpecificity())),

                // 코드 신뢰성: 에러 처리 + 단일 책임
                toHundred20(avg(
                        codeEval.getErrorHandling().getScore(),
                        codeEval.getSingleResponsibility().getScore())),

                // 커뮤니케이션: 설명 구조 + 구체성
                toHundred5(avg(
                        feedback.getScores().getStructure(),
                        feedback.getScores().getSpecificity())),

                // 설계 사고력: 네이밍 + 중복 제거 + 주석 품질
                toHundred20(avg(
                        codeEval.getNaming().getScore(),
                        codeEval.getDuplication().getScore(),
                        codeEval.getCommentQuality().getScore()))
        );
    }

    private int toHundred5(double score)  { return (int) Math.round((score / 5.0)  * 100); }
    private int toHundred20(double score) { return (int) Math.round((score / 20.0) * 100); }
    private double avg(int... values) { return Arrays.stream(values).average().orElse(0); }
}
