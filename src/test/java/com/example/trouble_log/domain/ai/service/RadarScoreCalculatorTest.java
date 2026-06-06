package com.example.trouble_log.domain.ai.service;

import com.example.trouble_log.domain.ai.dto.AnswerFeedbackResult;
import com.example.trouble_log.domain.ai.dto.CodeEvaluationResult;
import com.example.trouble_log.domain.ai.dto.RadarScore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RadarScoreCalculatorTest {

    private final RadarScoreCalculator calculator = new RadarScoreCalculator();

    @Test
    @DisplayName("레이더 점수 5개 카테고리 매핑 및 스케일링 검증")
    void calculate_returnsCorrectRadarScores() {
        // given — feedback
        AnswerFeedbackResult.Scores scores = new AnswerFeedbackResult.Scores();
        scores.setStructure(4);
        scores.setRelevance(2);
        scores.setKeyword(3);
        scores.setSpecificity(5);

        AnswerFeedbackResult feedback = new AnswerFeedbackResult();
        feedback.setScores(scores);

        // given — codeEval
        CodeEvaluationResult.Axis errorHandling = new CodeEvaluationResult.Axis();
        errorHandling.setScore(14);

        CodeEvaluationResult.Axis singleResp = new CodeEvaluationResult.Axis();
        singleResp.setScore(10);

        CodeEvaluationResult.Axis naming = new CodeEvaluationResult.Axis();
        naming.setScore(16);

        CodeEvaluationResult.Axis duplication = new CodeEvaluationResult.Axis();
        duplication.setScore(12);

        CodeEvaluationResult.Axis commentQuality = new CodeEvaluationResult.Axis();
        commentQuality.setScore(8);

        CodeEvaluationResult codeEval = new CodeEvaluationResult();
        codeEval.setErrorHandling(errorHandling);
        codeEval.setSingleResponsibility(singleResp);
        codeEval.setNaming(naming);
        codeEval.setDuplication(duplication);
        codeEval.setCommentQuality(commentQuality);

        // when
        RadarScore result = calculator.calculate(feedback, codeEval);

        // then
        // 문제해결력: avg(structure=4, relevance=2) = 3.0 → toHundred5 → 60
        assertThat(result.getProblemSolving()).isEqualTo(60);

        // 기술 판단력: avg(keyword=3, specificity=5) = 4.0 → toHundred5 → 80
        assertThat(result.getTechJudgment()).isEqualTo(80);

        // 코드 신뢰성: avg(errorHandling=14, singleResp=10) = 12.0 → toHundred20 → 60
        assertThat(result.getCodeReliability()).isEqualTo(60);

        // 커뮤니케이션: avg(structure=4, specificity=5) = 4.5 → toHundred5 → 90
        assertThat(result.getCommunication()).isEqualTo(90);

        // 설계 사고력: avg(naming=16, duplication=12, commentQuality=8) = 12.0 → toHundred20 → 60
        assertThat(result.getDesignThinking()).isEqualTo(60);
    }
}
