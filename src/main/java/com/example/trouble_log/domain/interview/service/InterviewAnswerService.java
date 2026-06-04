package com.example.trouble_log.domain.interview.service;

import com.example.trouble_log.domain.ai.dto.AnswerFeedbackResult;
import com.example.trouble_log.domain.ai.service.AzureOpenAiPromptService;
import com.example.trouble_log.domain.audit.service.AuditLogService;
import com.example.trouble_log.domain.interview.dto.InterviewAnswerRequest;
import com.example.trouble_log.domain.interview.dto.InterviewAnswerResponse;
import com.example.trouble_log.domain.interview.entity.InterviewAnswer;
import com.example.trouble_log.domain.interview.entity.InterviewQuestion;
import com.example.trouble_log.domain.interview.repository.InterviewAnswerRepository;
import com.example.trouble_log.domain.interview.repository.InterviewQuestionRepository;
import com.example.trouble_log.domain.projectSession.entity.PreContext;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewAnswerService {

    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final AzureOpenAiPromptService azureOpenAiPromptService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    // 질문별 답변을 저장하고 필요하면 AI 피드백까지 생성해 함께 반환한다.
    @Transactional
    public InterviewAnswerResponse saveAnswer(Long questionId, InterviewAnswerRequest request) {
        validateRequest(questionId, request);

        InterviewQuestion interviewQuestion = interviewQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "면접 질문을 찾을 수 없습니다."));

        validateOwnership(interviewQuestion, request.getMemberId());

        boolean isSkipped = Boolean.TRUE.equals(request.getIsSkipped());
        String normalizedAnswer = normalizeAnswer(request.getAnswer());

        if (!isSkipped && isBlank(normalizedAnswer)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "답변을 입력하거나 건너뛰기 여부를 선택해야 합니다.");
        }

        if (isSkipped && Boolean.TRUE.equals(request.getRequestFeedback())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "건너뛴 답변에 대해서는 AI 피드백을 요청할 수 없습니다.");
        }

        InterviewAnswer interviewAnswer = interviewAnswerRepository.findByInterviewQuestionId(questionId)
                .orElseGet(() -> new InterviewAnswer(interviewQuestion, normalizedAnswer, isSkipped));

        interviewAnswer.updateAnswer(normalizedAnswer, isSkipped);

        AnswerFeedbackResult feedbackResult = null;
        if (Boolean.TRUE.equals(request.getRequestFeedback())) {
            feedbackResult = createFeedback(interviewQuestion, normalizedAnswer, request.getMemberId());
            interviewAnswer.updateFeedback(serializeFeedback(feedbackResult));
        } else {
            interviewAnswer.updateFeedback(null);
        }

        InterviewAnswer savedAnswer = interviewAnswerRepository.save(interviewAnswer);

        return new InterviewAnswerResponse(
                interviewQuestion.getId(),
                interviewQuestion.getQuestionSequence(),
                interviewQuestion.getQuestion(),
                savedAnswer.getAnswer(),
                savedAnswer.getIsSkipped(),
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
    private void validateRequest(Long questionId, InterviewAnswerRequest request) {
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

    // 피드백 DTO를 DB 저장용 JSON 문자열로 직렬화한다.
    private String serializeFeedback(AnswerFeedbackResult feedbackResult) {
        try {
            return objectMapper.writeValueAsString(feedbackResult);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 피드백 저장 형식 변환에 실패했습니다.", e);
        }
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
