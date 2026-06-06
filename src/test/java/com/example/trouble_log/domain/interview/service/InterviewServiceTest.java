package com.example.trouble_log.domain.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.trouble_log.domain.ai.dto.AnswerFeedbackResult;
import com.example.trouble_log.domain.ai.service.AzureOpenAiPromptService;
import com.example.trouble_log.domain.ai.service.RadarScoreCalculator;
import com.example.trouble_log.domain.analysis.repository.AnalysisResultRepository;
import com.example.trouble_log.domain.interview.dto.AnswerRequest;
import com.example.trouble_log.domain.interview.dto.AnswerResponse;
import com.example.trouble_log.domain.interview.entity.InterviewAnswer;
import com.example.trouble_log.domain.interview.entity.InterviewQuestion;
import com.example.trouble_log.domain.interview.repository.InterviewAnswerRepository;
import com.example.trouble_log.domain.interview.repository.InterviewQuestionRepository;
import com.example.trouble_log.domain.projectSession.entity.PreContext;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import com.example.trouble_log.domain.projectSession.repository.PreContextRepository;
import com.example.trouble_log.domain.projectSession.repository.ProjectSessionRepository;
import com.example.trouble_log.domain.user.entity.Member;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class InterviewServiceTest {

    private InterviewAnswerRepository answerRepository;
    private AzureOpenAiPromptService promptService;
    private InterviewService interviewService;

    @BeforeEach
    void setUp() {
        InterviewQuestionRepository questionRepository = mock(InterviewQuestionRepository.class);
        answerRepository = mock(InterviewAnswerRepository.class);
        ProjectSessionRepository sessionRepository = mock(ProjectSessionRepository.class);
        PreContextRepository preContextRepository = mock(PreContextRepository.class);
        promptService = mock(AzureOpenAiPromptService.class);

        interviewService = new InterviewService(
                questionRepository,
                answerRepository,
                sessionRepository,
                preContextRepository,
                promptService,
                new ObjectMapper(),
                mock(RadarScoreCalculator.class),
                mock(AnalysisResultRepository.class)
        );

        ProjectSession projectSession = projectSession();
        InterviewQuestion question = new InterviewQuestion(projectSession, "질문입니다.", 1);
        ReflectionTestUtils.setField(question, "id", 1L);
        PreContext preContext = new PreContext(projectSession, "목적", "기술", "예외", "small");

        AnswerFeedbackResult feedbackResult = new AnswerFeedbackResult();
        feedbackResult.setImprovement("개선 제안");
        feedbackResult.setWarning("주의");

        when(sessionRepository.findById(anyLong())).thenReturn(Optional.of(projectSession));
        when(questionRepository.findById(anyLong())).thenReturn(Optional.of(question));
        when(preContextRepository.findByProjectSession(projectSession)).thenReturn(Optional.of(preContext));
        when(promptService.evaluateAnswer(any(ProjectSession.class), any(PreContext.class), any(), any()))
                .thenReturn(feedbackResult);
        when(answerRepository.save(any(InterviewAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void saveAnswerUpdatesExistingAnswerInsteadOfCreatingDuplicate() {
        InterviewAnswer existingAnswer = new InterviewAnswer(question(), null, true, null);
        ReflectionTestUtils.setField(existingAnswer, "id", 10L);
        when(answerRepository.findByInterviewQuestionId(1L)).thenReturn(Optional.of(existingAnswer));

        AnswerRequest request = new AnswerRequest();
        request.setAnswer("수정된 답변입니다.");

        AnswerResponse response = interviewService.saveAnswer(1L, 1L, request);

        assertThat(response.getAnswerId()).isEqualTo(10L);
        assertThat(existingAnswer.getAnswer()).isEqualTo("수정된 답변입니다.");
        assertThat(existingAnswer.isSkipped()).isFalse();
        assertThat(existingAnswer.getFeedback()).isNotBlank();
    }

    @Test
    void saveAnswerReturnsDefaultImprovementWhenAiImprovementIsNull() {
        AnswerFeedbackResult feedbackResult = new AnswerFeedbackResult();
        feedbackResult.setImprovement(null);
        when(promptService.evaluateAnswer(any(ProjectSession.class), any(PreContext.class), any(), any()))
                .thenReturn(feedbackResult);

        InterviewAnswer existingAnswer = new InterviewAnswer(question(), null, true, null);
        ReflectionTestUtils.setField(existingAnswer, "id", 10L);
        when(answerRepository.findByInterviewQuestionId(1L)).thenReturn(Optional.of(existingAnswer));

        AnswerRequest request = new AnswerRequest();
        request.setAnswer("좋은 답변입니다.");

        AnswerResponse response = interviewService.saveAnswer(1L, 1L, request);

        assertThat(response.getImprovement()).isEqualTo(
                "현재 답변은 질문의 핵심을 잘 짚고 있어요. 이 흐름을 유지하면서 본인이 직접 고민하고 해결한 과정까지 차분히 이어가면 더 설득력 있는 답변이 될 수 있습니다."
        );
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
}
