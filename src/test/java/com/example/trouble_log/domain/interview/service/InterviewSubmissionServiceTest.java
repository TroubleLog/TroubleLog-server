package com.example.trouble_log.domain.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.trouble_log.domain.audit.service.AuditLogService;
import com.example.trouble_log.domain.interview.dto.InterviewSubmitAnswerRequest;
import com.example.trouble_log.domain.interview.dto.InterviewSubmitRequest;
import com.example.trouble_log.domain.interview.dto.InterviewSubmitResponse;
import com.example.trouble_log.domain.interview.entity.InterviewAnswer;
import com.example.trouble_log.domain.interview.entity.InterviewQuestion;
import com.example.trouble_log.domain.interview.repository.InterviewAnswerRepository;
import com.example.trouble_log.domain.interview.repository.InterviewQuestionRepository;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import com.example.trouble_log.domain.projectSession.repository.ProjectSessionRepository;
import com.example.trouble_log.domain.user.entity.Member;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class InterviewSubmissionServiceTest {

    private ProjectSessionRepository projectSessionRepository;
    private InterviewQuestionRepository interviewQuestionRepository;
    private InterviewAnswerRepository interviewAnswerRepository;
    private InterviewSubmissionService interviewSubmissionService;

    @BeforeEach
    void setUp() {
        projectSessionRepository = mock(ProjectSessionRepository.class);
        interviewQuestionRepository = mock(InterviewQuestionRepository.class);
        interviewAnswerRepository = mock(InterviewAnswerRepository.class);
        interviewSubmissionService = new InterviewSubmissionService(
                projectSessionRepository,
                interviewQuestionRepository,
                interviewAnswerRepository,
                mock(AuditLogService.class)
        );

        when(interviewAnswerRepository.findByInterviewQuestionId(anyLong())).thenReturn(Optional.empty());
        when(interviewAnswerRepository.save(any(InterviewAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void submitDoesNotBlockWhenAnswerContainsPersonalInfo() {
        ProjectSession projectSession = projectSession();
        InterviewSubmitRequest request = request(List.of(
                answerRequest(1L, "제 이메일은 test@example.com 입니다."),
                answerRequest(2L, "답변입니다."),
                answerRequest(3L, "답변입니다.")
        ));
        List<InterviewQuestion> questions = List.of(
                question(projectSession, 1L, "질문 1", 1),
                question(projectSession, 2L, "질문 2", 2),
                question(projectSession, 3L, "질문 3", 3)
        );

        when(projectSessionRepository.findById(anyLong())).thenReturn(Optional.of(projectSession));
        when(interviewQuestionRepository.findByProjectSessionIdOrderByQuestionSequenceAsc(anyLong()))
                .thenReturn(questions);

        InterviewSubmitResponse response = interviewSubmissionService.submit(1L, request);

        assertThat(response.getSubmitted()).isTrue();
        assertThat(response.getReportGenerationReady()).isTrue();
    }

    @Test
    void submitReturnsReadyWhenAllAnswersDoNotContainPersonalInfo() {
        ProjectSession projectSession = projectSession();
        InterviewSubmitRequest request = request(List.of(
                answerRequest(1L, "트랜잭션 경계를 서비스 계층에 두었습니다."),
                answerRequest(2L, "JPA 변경 감지를 활용했습니다."),
                answerRequest(3L, "예외는 도메인별로 구분해 처리했습니다.")
        ));
        List<InterviewQuestion> questions = List.of(
                question(projectSession, 1L, "질문 1", 1),
                question(projectSession, 2L, "질문 2", 2),
                question(projectSession, 3L, "질문 3", 3)
        );

        when(projectSessionRepository.findById(anyLong())).thenReturn(Optional.of(projectSession));
        when(interviewQuestionRepository.findByProjectSessionIdOrderByQuestionSequenceAsc(anyLong()))
                .thenReturn(questions);

        InterviewSubmitResponse response = interviewSubmissionService.submit(1L, request);

        assertThat(response.getSubmitted()).isTrue();
        assertThat(response.getReportGenerationReady()).isTrue();
        assertThat(response.getBlockedReason()).isNull();
        assertThat(response.getWarnings()).isEmpty();
    }

    @Test
    void submitAllowsBlankFinalAnswerAsUnanswered() {
        ProjectSession projectSession = projectSession();
        InterviewSubmitRequest request = request(List.of(
                answerRequest(1L, ""),
                answerRequest(2L, "JPA 변경 감지를 활용했습니다."),
                answerRequest(3L, "예외는 도메인별로 구분해 처리했습니다.")
        ));
        List<InterviewQuestion> questions = List.of(
                question(projectSession, 1L, "질문 1", 1),
                question(projectSession, 2L, "질문 2", 2),
                question(projectSession, 3L, "질문 3", 3)
        );

        when(projectSessionRepository.findById(anyLong())).thenReturn(Optional.of(projectSession));
        when(interviewQuestionRepository.findByProjectSessionIdOrderByQuestionSequenceAsc(anyLong()))
                .thenReturn(questions);

        InterviewSubmitResponse response = interviewSubmissionService.submit(1L, request);

        assertThat(response.getSubmitted()).isTrue();
        assertThat(response.getReportGenerationReady()).isTrue();
    }

    @Test
    void submitUpdatesExistingSkippedAnswerAsAnswered() {
        ProjectSession projectSession = projectSession();
        InterviewAnswer existingAnswer = new InterviewAnswer(question(projectSession, 1L, "질문 1", 1), null, true, null);
        ReflectionTestUtils.setField(existingAnswer, "id", 10L);

        InterviewSubmitRequest request = request(List.of(
                answerRequest(1L, "최종 답변입니다."),
                answerRequest(2L, "JPA 변경 감지를 활용했습니다."),
                answerRequest(3L, "예외는 도메인별로 구분해 처리했습니다.")
        ));
        List<InterviewQuestion> questions = List.of(
                existingAnswer.getInterviewQuestion(),
                question(projectSession, 2L, "질문 2", 2),
                question(projectSession, 3L, "질문 3", 3)
        );

        when(projectSessionRepository.findById(anyLong())).thenReturn(Optional.of(projectSession));
        when(interviewQuestionRepository.findByProjectSessionIdOrderByQuestionSequenceAsc(anyLong()))
                .thenReturn(questions);
        when(interviewAnswerRepository.findByInterviewQuestionId(1L)).thenReturn(Optional.of(existingAnswer));

        InterviewSubmitResponse response = interviewSubmissionService.submit(1L, request);

        assertThat(response.getSubmitted()).isTrue();
        assertThat(existingAnswer.getAnswer()).isEqualTo("최종 답변입니다.");
        assertThat(existingAnswer.isSkipped()).isFalse();
    }

    private ProjectSession projectSession() {
        Member member = new Member("user@example.com", "password", "user");
        ReflectionTestUtils.setField(member, "id", 1L);

        ProjectSession projectSession = new ProjectSession(member, "code", null);
        ReflectionTestUtils.setField(projectSession, "id", 1L);
        return projectSession;
    }

    private InterviewSubmitRequest request(List<InterviewSubmitAnswerRequest> answers) {
        InterviewSubmitRequest request = new InterviewSubmitRequest();
        ReflectionTestUtils.setField(request, "memberId", 1L);
        ReflectionTestUtils.setField(request, "answers", answers);
        return request;
    }

    private InterviewSubmitAnswerRequest answerRequest(Long questionId, String answerText) {
        InterviewSubmitAnswerRequest request = new InterviewSubmitAnswerRequest();
        ReflectionTestUtils.setField(request, "questionId", questionId);
        ReflectionTestUtils.setField(request, "answer", answerText);
        return request;
    }

    private InterviewQuestion question(
            ProjectSession projectSession,
            Long id,
            String questionText,
            int sequence
    ) {
        InterviewQuestion question = new InterviewQuestion(projectSession, questionText, sequence);
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }
}
