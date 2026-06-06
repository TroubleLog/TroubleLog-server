package com.example.trouble_log.domain.analysis.entity;

import com.example.trouble_log.domain.ai.dto.CodeEvaluationResult;
import com.example.trouble_log.domain.ai.dto.RadarScore;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "analysis_result")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private ProjectSession projectSession;

    // --- 레이더 차트 스코어 영역 ---
    @Column(nullable = false)
    private Integer scoreProblemSolving;

    @Column(nullable = false)
    private Integer scoreTechJudgment;

    @Column(nullable = false)
    private Integer scoreCodeReliability;

    @Column(nullable = false)
    private Integer scoreCommunication;

    @Column(nullable = false)
    private Integer scoreArchitecture;

    // --- 클린코드 평가 스코어 영역 ---
    @Column(nullable = false)
    private int scoreNaming;

    @Column(nullable = false)
    private int scoreSingleResponsibility;

    @Column(nullable = false)
    private int scoreErrorHandling;

    @Column(nullable = false)
    private int scoreDuplication;

    @Column(nullable = false)
    private int scoreCommentQuality;

    // --- 트러블 슈팅 리포트 영역 ---
    @Column(columnDefinition = "TEXT", nullable = false)
    private String reportBackground;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reportProblem;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reportCause;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reportSolution;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reportResult;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 레이더 차트와 리포트 전체 저장용 생성자
    public AnalysisResult(ProjectSession projectSession,
                          RadarScore radarScore,
                          CodeEvaluationResult codeEval,
                          String reportBackground,
                          String reportProblem,
                          String reportCause,
                          String reportSolution,
                          String reportResult) {
        this.projectSession = projectSession;
        this.scoreProblemSolving = radarScore.getProblemSolving();
        this.scoreTechJudgment = radarScore.getTechJudgment();
        this.scoreCodeReliability = radarScore.getCodeReliability();
        this.scoreCommunication = radarScore.getCommunication();
        this.scoreArchitecture = radarScore.getDesignThinking();
        this.scoreNaming = codeEval.getNaming().getScore();
        this.scoreSingleResponsibility = codeEval.getSingleResponsibility().getScore();
        this.scoreErrorHandling = codeEval.getErrorHandling().getScore();
        this.scoreDuplication = codeEval.getDuplication().getScore();
        this.scoreCommentQuality = codeEval.getCommentQuality().getScore();
        this.reportBackground = reportBackground;
        this.reportProblem = reportProblem;
        this.reportCause = reportCause;
        this.reportSolution = reportSolution;
        this.reportResult = reportResult;
        this.createdAt = LocalDateTime.now();
    }
}
