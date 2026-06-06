package com.example.trouble_log.domain.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.trouble_log.domain.ai.dto.AnswerFeedbackResult;
import com.example.trouble_log.domain.ai.dto.CodeEvaluationResult;
import com.example.trouble_log.domain.ai.dto.RadarScore;
import com.example.trouble_log.domain.ai.service.AzureOpenAiPromptService;
import com.example.trouble_log.domain.ai.service.RadarScoreCalculator;
import com.example.trouble_log.domain.analysis.entity.AnalysisResult;
import com.example.trouble_log.domain.analysis.repository.AnalysisResultRepository;
import com.example.trouble_log.domain.interview.dto.AnswerRequest;
import com.example.trouble_log.domain.interview.dto.AnswerResponse;
import com.example.trouble_log.domain.interview.dto.ReportResponse;
import com.example.trouble_log.domain.interview.entity.InterviewAnswer;
import com.example.trouble_log.domain.interview.entity.InterviewQuestion;
import com.example.trouble_log.domain.interview.repository.InterviewQuestionRepository;
import com.example.trouble_log.domain.projectSession.entity.PreContext;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import com.example.trouble_log.domain.projectSession.repository.PreContextRepository;
import com.example.trouble_log.domain.projectSession.repository.ProjectSessionRepository;
import com.example.trouble_log.domain.user.entity.Member;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class InterviewServiceTest {

    private InterviewQuestionRepository questionRepository;
    private AzureOpenAiPromptService promptService;
    private RadarScoreCalculator radarCalculator;
    private AnalysisResultRepository analysisResultRepository;
    private InterviewService interviewService;
    private ProjectSession projectSession;
    private PreContext preContext;

    @BeforeEach
    void setUp() {
        questionRepository = mock(InterviewQuestionRepository.class);
        ProjectSessionRepository sessionRepository = mock(ProjectSessionRepository.class);
        PreContextRepository preContextRepository = mock(PreContextRepository.class);
        promptService = mock(AzureOpenAiPromptService.class);
        radarCalculator = mock(RadarScoreCalculator.class);
        analysisResultRepository = mock(AnalysisResultRepository.class);

        interviewService = new InterviewService(
                questionRepository,
                sessionRepository,
                preContextRepository,
                promptService,
                radarCalculator,
                analysisResultRepository
        );

        projectSession = projectSession();
        InterviewQuestion question = new InterviewQuestion(projectSession, "질문입니다.", 1);
        ReflectionTestUtils.setField(question, "id", 1L);
        preContext = new PreContext(projectSession, "목적", "기술", "예외", "small");
        ReflectionTestUtils.setField(projectSession, "preContext", preContext);

        AnswerFeedbackResult feedbackResult = new AnswerFeedbackResult();
        feedbackResult.setScores(answerScores());
        feedbackResult.setImprovement("개선 제안");
        feedbackResult.setWarning("주의");

        when(sessionRepository.findById(anyLong())).thenReturn(Optional.of(projectSession));
        when(questionRepository.findById(anyLong())).thenReturn(Optional.of(question));
        when(preContextRepository.findByProjectSession(projectSession)).thenReturn(Optional.of(preContext));
        when(promptService.evaluateAnswer(any(ProjectSession.class), any(PreContext.class), any(), any()))
                .thenReturn(feedbackResult);
        when(promptService.evaluateCode(any())).thenReturn(codeEvaluationResult());
        when(promptService.generateReport(any(), any(), any(), any(), any(), any()))
                .thenReturn("## Background\n배경\n## Problem\n문제\n## Root Cause\n원인\n## Resolution\n해결\n## Result\n결과");
        when(radarCalculator.calculate(any(AnswerFeedbackResult.class), any(CodeEvaluationResult.class)))
                .thenReturn(new RadarScore(80, 80, 80, 80, 80));
    }

    @Test
    void saveAnswerReturnsFeedbackWithoutSavingAnswer() {
        AnswerRequest request = new AnswerRequest();
        request.setAnswer("수정된 답변입니다.");

        AnswerResponse response = interviewService.saveAnswer(1L, 1L, request);

        assertThat(response.getAnswerId()).isNull();
        assertThat(response.getImprovement()).isEqualTo("개선 제안");
        assertThat(response.getWarning()).isEqualTo("주의");
    }

    @Test
    void saveAnswerReturnsDefaultImprovementWhenAiImprovementIsNull() {
        AnswerFeedbackResult feedbackResult = new AnswerFeedbackResult();
        feedbackResult.setImprovement(null);
        when(promptService.evaluateAnswer(any(ProjectSession.class), any(PreContext.class), any(), any()))
                .thenReturn(feedbackResult);

        AnswerRequest request = new AnswerRequest();
        request.setAnswer("좋은 답변입니다.");

        AnswerResponse response = interviewService.saveAnswer(1L, 1L, request);

        assertThat(response.getImprovement()).isEqualTo(
                "현재 답변은 질문의 핵심을 잘 짚고 있어요. 이 흐름을 유지하면서 본인이 직접 고민하고 해결한 과정까지 차분히 이어가면 더 설득력 있는 답변이 될 수 있습니다."
        );
    }

    @Test
    void saveAnswerReturnsDefaultImprovementWhenAiImprovementIsBlank() {
        AnswerFeedbackResult feedbackResult = new AnswerFeedbackResult();
        feedbackResult.setImprovement("   ");
        when(promptService.evaluateAnswer(any(ProjectSession.class), any(PreContext.class), any(), any()))
                .thenReturn(feedbackResult);

        AnswerRequest request = new AnswerRequest();
        request.setAnswer("좋은 답변입니다.");

        AnswerResponse response = interviewService.saveAnswer(1L, 1L, request);

        assertThat(response.getImprovement()).isEqualTo(
                "현재 답변은 질문의 핵심을 잘 짚고 있어요. 이 흐름을 유지하면서 본인이 직접 고민하고 해결한 과정까지 차분히 이어가면 더 설득력 있는 답변이 될 수 있습니다."
        );
    }

    @Test
    void generateReportCalculatesRadarScoreFromFinalAnswers() {
        InterviewQuestion question = new InterviewQuestion(projectSession, "질문입니다.", 1);
        ReflectionTestUtils.setField(question, "id", 1L);
        InterviewAnswer finalAnswer = new InterviewAnswer(question, "최종 답변입니다.", false, null);
        ReflectionTestUtils.setField(question, "interviewAnswer", finalAnswer);

        when(questionRepository.findByProjectSessionIdOrderByQuestionSequenceAsc(1L))
                .thenReturn(List.of(question));

        ReportResponse response = interviewService.generateReport(1L);

        assertThat(response.getRadarScore()).isNotNull();
        verify(promptService).evaluateAnswer(projectSession, preContext, "질문입니다.", "최종 답변입니다.");
        verify(radarCalculator).calculate(any(AnswerFeedbackResult.class), any(CodeEvaluationResult.class));
        verify(analysisResultRepository).save(any(AnalysisResult.class));
    }

    private ProjectSession projectSession() {
        Member member = new Member("user@example.com", "password", "user");
        ReflectionTestUtils.setField(member, "id", 1L);

        ProjectSession projectSession = new ProjectSession(member, "code", null);
        ReflectionTestUtils.setField(projectSession, "id", 1L);
        return projectSession;
    }

    private InterviewQuestion question() {
        InterviewQuestion question = new InterviewQuestion(projectSession(), "질문입니다.", 1);
        ReflectionTestUtils.setField(question, "id", 1L);
        return question;
    }

    private CodeEvaluationResult codeEvaluationResult() {
        CodeEvaluationResult result = new CodeEvaluationResult();
        result.setNaming(axis());
        result.setSingleResponsibility(axis());
        result.setErrorHandling(axis());
        result.setDuplication(axis());
        result.setCommentQuality(axis());
        return result;
    }

    private AnswerFeedbackResult.Scores answerScores() {
        AnswerFeedbackResult.Scores scores = new AnswerFeedbackResult.Scores();
        scores.setSpecificity(4);
        scores.setStructure(4);
        scores.setRelevance(4);
        scores.setKeyword(4);
        return scores;
    }

    private CodeEvaluationResult.Axis axis() {
        CodeEvaluationResult.Axis axis = new CodeEvaluationResult.Axis();
        axis.setScore(10);
        return axis;
    }
}
