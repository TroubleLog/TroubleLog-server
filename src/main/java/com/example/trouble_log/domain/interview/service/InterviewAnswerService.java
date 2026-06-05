package com.example.trouble_log.domain.interview.service;

import com.example.trouble_log.domain.ai.dto.AnswerFeedbackResult;
import com.example.trouble_log.domain.ai.service.AzureOpenAiPromptService;
import com.example.trouble_log.domain.audit.service.AuditLogService;
import com.example.trouble_log.domain.interview.dto.InterviewAnswerFeedbackRequest;
import com.example.trouble_log.domain.interview.dto.InterviewAnswerFeedbackResponse;
import com.example.trouble_log.domain.interview.entity.InterviewQuestion;
import com.example.trouble_log.domain.interview.repository.InterviewQuestionRepository;
import com.example.trouble_log.domain.projectSession.entity.PreContext;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InterviewAnswerService {

    private final InterviewQuestionRepository interviewQuestionRepository;
    private final AzureOpenAiPromptService azureOpenAiPromptService;
    private final AuditLogService auditLogService;

    // 질문별 현재 답변을 저장하지 않고 AI 피드백만 생성해 반환한다.
    public InterviewAnswerFeedbackResponse createFeedback(Long questionId, InterviewAnswerFeedbackRequest request) {
        validateRequest(questionId, request);

        InterviewQuestion interviewQuestion = interviewQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "면접 질문을 찾을 수 없습니다."));

        validateOwnership(interviewQuestion, request.getMemberId());

        String normalizedAnswer = normalizeAnswer(request.getAnswer());
        if (isBlank(normalizedAnswer)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI 피드백을 받으려면 답변을 입력해야 합니다.");
        }

        AnswerFeedbackResult feedbackResult = createFeedback(interviewQuestion, normalizedAnswer, request.getMemberId());

        return new InterviewAnswerFeedbackResponse(
                interviewQuestion.getId(),
                interviewQuestion.getQuestionSequence(),
                feedbackResult
        );
    }

    // 답변 피드백 생성에 필요한 프로젝트 세션과 사전 컨텍스트를 모아 OpenAI를 호출한다.
    private AnswerFeedbackResult createFeedback(InterviewQuestion interviewQuestion, String answer, Long memberId) {
        ProjectSession projectSession = interviewQuestion.getProjectSession();
        PreContext preContext = projectSession.getPreContext();

        if (preContext == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사전 컨텍스트가 없어 AI 피드백을 생성할 수 없습니다.");
        }

        Long sessionId = projectSession.getId();
        String requestSummary = buildFeedbackRequestSummary(interviewQuestion, answer, projectSession, preContext);
        auditLogService.recordStarted(memberId, sessionId, "AI_INTERVIEW_ANSWER_FEEDBACK", requestSummary);

        try {
            AnswerFeedbackResult result = azureOpenAiPromptService.evaluateAnswer(
                    projectSession,
                    preContext,
                    interviewQuestion.getQuestion(),
                    answer
            );

            auditLogService.recordSuccess(
                    memberId,
                    sessionId,
                    "AI_INTERVIEW_ANSWER_FEEDBACK",
                    requestSummary,
                    buildFeedbackResponseSummary(result)
            );
            return result;
        } catch (RuntimeException e) {
            auditLogService.recordFailure(
                    memberId,
                    sessionId,
                    "AI_INTERVIEW_ANSWER_FEEDBACK",
                    requestSummary,
                    e.getMessage()
            );
            throw e;
        }
    }

    // 요청 본문과 경로 변수에 필요한 값이 모두 존재하는지 확인한다.
    private void validateRequest(Long questionId, InterviewAnswerFeedbackRequest request) {
        if (questionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문 ID는 필수입니다.");
        }

        if (request == null || request.getMemberId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회원 ID는 필수입니다.");
        }
    }

    // 요청한 회원이 해당 질문이 속한 프로젝트 세션의 소유자인지 확인한다.
    private void validateOwnership(InterviewQuestion interviewQuestion, Long memberId) {
        if (!interviewQuestion.getProjectSession().getMember().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 면접 질문에 접근할 수 없습니다.");
        }
    }

    // 프론트에서 넘어온 답변을 저장용 문자열로 정규화한다.
    private String normalizeAnswer(String answer) {
        return isBlank(answer) ? null : answer.trim();
    }

    // AI 피드백 요청 내용을 감사 로그용 요약 문자열로 변환한다.
    private String buildFeedbackRequestSummary(
            InterviewQuestion interviewQuestion,
            String answer,
            ProjectSession projectSession,
            PreContext preContext
    ) {
        return "questionId=%d, questionSequence=%d, answerLength=%d, codeLength=%d, codePurposeLength=%d, techRationaleLength=%d, exceptionHandlingLength=%d, projectScale=%s"
                .formatted(
                        interviewQuestion.getId(),
                        interviewQuestion.getQuestionSequence(),
                        lengthOf(answer),
                        lengthOf(projectSession.getCodeContent()),
                        lengthOf(preContext.getCodePurpose()),
                        lengthOf(preContext.getTechRationale()),
                        lengthOf(preContext.getExceptionHandling()),
                        preContext.getProjectScale()
                );
    }

    // AI 피드백 응답의 핵심 점수를 감사 로그용 요약 문자열로 변환한다.
    private String buildFeedbackResponseSummary(AnswerFeedbackResult feedbackResult) {
        return "specificity=%d, structure=%d, relevance=%d, keyword=%d"
                .formatted(
                        feedbackResult.getScores().getSpecificity(),
                        feedbackResult.getScores().getStructure(),
                        feedbackResult.getScores().getRelevance(),
                        feedbackResult.getScores().getKeyword()
                );
    }

    // 문자열이 null 이거나 공백만 포함하는지 확인한다.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // 문자열 길이를 반환하고 값이 없으면 0으로 처리한다.
    private int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }
}
