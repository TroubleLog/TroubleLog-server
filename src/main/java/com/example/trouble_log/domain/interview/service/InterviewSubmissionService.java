package com.example.trouble_log.domain.interview.service;

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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewSubmissionService {

    private static final int REQUIRED_QUESTION_COUNT = 3;

    private final ProjectSessionRepository projectSessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final AuditLogService auditLogService;

    // 최종 답변 3개를 저장한 뒤 리포트 생성 가능 상태를 반환한다.
    @Transactional
    public InterviewSubmitResponse submit(Long sessionId, InterviewSubmitRequest request) {
        validateRequest(sessionId, request);

        ProjectSession projectSession = projectSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트 세션을 찾지 못했습니다."));

        validateOwnership(projectSession, request.getMemberId());

        List<InterviewQuestion> questions = interviewQuestionRepository.findByProjectSessionIdOrderByQuestionSequenceAsc(sessionId);
        validateQuestionCount(questions);

        Map<Long, String> finalAnswers = validateAndNormalizeFinalAnswers(questions, request.getAnswers());
        List<InterviewAnswer> savedAnswers = saveFinalAnswers(questions, finalAnswers);

        auditLogService.recordSuccess(
                request.getMemberId(),
                sessionId,
                "INTERVIEW_ANSWER_SUBMIT",
                buildSubmitRequestSummary(questions, savedAnswers),
                "reportGenerationReady=true"
        );

        return new InterviewSubmitResponse(true, true, null, List.of());
    }

    // 요청 본문과 경로 변수에 필요한 값이 모두 존재하는지 확인한다.
    private void validateRequest(Long sessionId, InterviewSubmitRequest request) {
        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로젝트 세션 ID는 필수입니다.");
        }

        if (request == null || request.getMemberId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회원 ID는 필수입니다.");
        }

        if (request.getAnswers() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최종 답변 목록은 필수입니다.");
        }
    }

    // 요청한 회원이 해당 프로젝트 세션의 소유자인지 확인한다.
    private void validateOwnership(ProjectSession projectSession, Long memberId) {
        if (!projectSession.getMember().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 프로젝트 세션에 접근할 수 없습니다.");
        }
    }

    // 세션에 리포트 생성을 위한 면접 질문 3개가 준비되어 있는지 확인한다.
    private void validateQuestionCount(List<InterviewQuestion> questions) {
        if (questions.size() != REQUIRED_QUESTION_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "면접 질문 3개가 모두 생성된 뒤 제출할 수 있습니다.");
        }
    }

    // 요청으로 들어온 최종 답변이 세션의 질문 3개와 정확히 매칭되는지 확인하고 저장 가능한 문자열로 정규화한다.
    private Map<Long, String> validateAndNormalizeFinalAnswers(
            List<InterviewQuestion> questions,
            List<InterviewSubmitAnswerRequest> answers
    ) {
        if (answers.size() != REQUIRED_QUESTION_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최종 답변 3개를 모두 제출해야 합니다.");
        }

        Set<Long> questionIds = questions.stream()
                .map(InterviewQuestion::getId)
                .collect(Collectors.toSet());
        Set<Long> requestedQuestionIds = new HashSet<>();

        Map<Long, String> finalAnswers = new LinkedHashMap<>();
        for (InterviewSubmitAnswerRequest answer : answers) {
            validateAnswerRequest(answer, questionIds, requestedQuestionIds);
            finalAnswers.put(answer.getQuestionId(), normalizeAnswer(answer.getAnswer()));
        }

        if (finalAnswers.size() != REQUIRED_QUESTION_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "중복되지 않은 최종 답변 3개를 제출해야 합니다.");
        }

        return finalAnswers;
    }

    // 답변 항목 하나가 유효한 질문 ID를 포함하는지 확인한다.
    private void validateAnswerRequest(
            InterviewSubmitAnswerRequest answer,
            Set<Long> questionIds,
            Set<Long> requestedQuestionIds
    ) {
        if (answer == null || answer.getQuestionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문 ID는 필수입니다.");
        }

        if (!questionIds.contains(answer.getQuestionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 프로젝트 세션에 속하지 않은 질문이 포함되어 있습니다.");
        }

        if (!requestedQuestionIds.add(answer.getQuestionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "중복된 질문 답변이 포함되어 있습니다.");
        }
    }

    // 프론트에서 넘어온 답변을 저장용 문자열로 정규화한다.
    private String normalizeAnswer(String answer) {
        return answer == null || answer.isBlank() ? null : answer.trim();
    }

    // 최종 제출 답변을 DB에 반영한다.
    private List<InterviewAnswer> saveFinalAnswers(List<InterviewQuestion> questions, Map<Long, String> finalAnswers) {
        return questions.stream()
                .map(question -> saveFinalAnswer(question, finalAnswers.get(question.getId())))
                .toList();
    }

    // 질문 하나의 최종 답변을 새로 저장하거나 기존 답변 레코드에 덮어쓴다.
    private InterviewAnswer saveFinalAnswer(InterviewQuestion question, String finalAnswer) {
        InterviewAnswer interviewAnswer = interviewAnswerRepository.findByInterviewQuestionId(question.getId())
                .orElseGet(() -> new InterviewAnswer(question, finalAnswer));

        interviewAnswer.updateAnswer(finalAnswer);

        return interviewAnswerRepository.save(interviewAnswer);
    }

    // 최종 제출 요청 내용을 감사 로그용 요약 문자열로 변환한다.
    private String buildSubmitRequestSummary(List<InterviewQuestion> questions, List<InterviewAnswer> answers) {
        int answerLength = answers.stream()
                .map(InterviewAnswer::getAnswer)
                .mapToInt(this::lengthOf)
                .sum();

        return "questionCount=%d, answerLength=%d".formatted(questions.size(), answerLength);
    }

    // 문자열 길이를 반환하고 값이 없으면 0으로 처리한다.
    private int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }
}
