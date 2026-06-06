package com.example.trouble_log.domain.projectSession.service;

import com.example.trouble_log.domain.ai.service.AzureOpenAiPromptService;
import com.example.trouble_log.domain.audit.service.AuditLogService;
import com.example.trouble_log.domain.interview.dto.InterviewQuestionResponse;
import com.example.trouble_log.domain.interview.dto.PersonalInfoWarning;
import com.example.trouble_log.domain.interview.entity.InterviewQuestion;
import com.example.trouble_log.domain.interview.exception.PersonalInfoDetectedException;
import com.example.trouble_log.domain.interview.repository.InterviewQuestionRepository;
import com.example.trouble_log.domain.interview.service.PersonalInfoDetectionService;
import com.example.trouble_log.domain.projectSession.dto.PreContextRequest;
import com.example.trouble_log.domain.projectSession.dto.PreContextResponse;
import com.example.trouble_log.domain.projectSession.dto.ProjectSessionRequest;
import com.example.trouble_log.domain.projectSession.dto.ProjectSessionResponse;
import com.example.trouble_log.domain.projectSession.entity.PreContext;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import com.example.trouble_log.domain.projectSession.repository.PreContextRepository;
import com.example.trouble_log.domain.projectSession.repository.ProjectSessionRepository;
import com.example.trouble_log.domain.user.entity.Member;
import com.example.trouble_log.domain.user.repository.MemberRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectSessionService {

    private final MemberRepository memberRepository;
    private final ProjectSessionRepository projectSessionRepository;
    private final PreContextRepository preContextRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final AzureOpenAiPromptService azureOpenAiPromptService;
    private final AuditLogService auditLogService;
    private final PersonalInfoDetectionService personalInfoDetectionService;

    @Transactional
    // 회원의 코드와 GitHub URL을 기반으로 새 프로젝트 세션을 생성한다.
    public ProjectSessionResponse create(ProjectSessionRequest request) {
        validateRequestBody(request);
        validateRequiredFields("회원 ID와 소스코드는 필수입니다.", request.getMemberId(), request.getCodeContent());
        validateCodeContentDoesNotContainPersonalInfo(request);

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> {
                    auditLogService.recordFailure(
                            request.getMemberId(),
                            null,
                            "PROJECT_SESSION_CREATE",
                            buildProjectSessionRequestSummary(request),
                            "회원을 찾지 못했습니다."
                    );
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾지 못했습니다.");
                });

        ProjectSession projectSession = new ProjectSession(
                member,
                request.getCodeContent(),
                request.getGithubUrl()
        );

        ProjectSession savedProjectSession = projectSessionRepository.save(projectSession);

        runAfterCommit(() -> auditLogService.recordSuccess(
                member.getId(),
                savedProjectSession.getId(),
                "PROJECT_SESSION_CREATE",
                buildProjectSessionRequestSummary(request),
                "sessionId=" + savedProjectSession.getId()
        ));

        return new ProjectSessionResponse(savedProjectSession.getId());
    }

    private void validateCodeContentDoesNotContainPersonalInfo(ProjectSessionRequest request) {
        List<PersonalInfoWarning> warnings = personalInfoDetectionService.detect(request.getCodeContent());
        if (warnings.isEmpty()) {
            return;
        }

        auditLogService.recordFailure(
                request.getMemberId(),
                null,
                "PROJECT_SESSION_CREATE",
                buildProjectSessionRequestSummary(request),
                "개인정보 감지: " + warnings.stream().map(PersonalInfoWarning::getType).toList()
        );
        throw new PersonalInfoDetectedException(warnings);
    }

    @Transactional
    // 사전 컨텍스트를 저장하고 AI 면접 질문을 생성해 함께 반환한다.
    public PreContextResponse createPreContext(Long sessionId, PreContextRequest request) {
        validateRequestBody(request);
        validateRequiredFields(
                "회원 ID, 사전 컨텍스트 정보는 필수입니다.",
                request.getMemberId(),
                request.getCodePurpose(),
                request.getTechRationale(),
                request.getExceptionHandling(),
                request.getProjectScale()
        );

        ProjectSession projectSession = projectSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트 세션을 찾지 못했습니다."));

        if (!projectSession.getMember().getId().equals(request.getMemberId())) {
            auditLogService.recordFailure(
                    request.getMemberId(),
                    sessionId,
                    "PRE_CONTEXT_CREATE",
                    buildPreContextRequestSummary(request),
                    "해당 프로젝트 세션에 접근할 수 없습니다."
            );
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 프로젝트 세션에 접근할 수 없습니다.");
        }

        PreContext preContext = new PreContext(
                projectSession,
                request.getCodePurpose(),
                request.getTechRationale(),
                request.getExceptionHandling(),
                request.getProjectScale()
        );

        PreContext savedPreContext;
        try {
            savedPreContext = preContextRepository.saveAndFlush(preContext);
        } catch (DataIntegrityViolationException e) {
            auditLogService.recordFailure(
                    request.getMemberId(),
                    sessionId,
                    "PRE_CONTEXT_CREATE",
                    buildPreContextRequestSummary(request),
                    "이미 사전 컨텍스트가 저장된 프로젝트 세션입니다."
            );
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사전 컨텍스트가 저장된 프로젝트 세션입니다.");
        }

        auditLogService.recordStarted(
                request.getMemberId(),
                sessionId,
                "AI_INTERVIEW_QUESTION_GENERATE",
                buildAiQuestionRequestSummary(projectSession, savedPreContext)
        );

        List<String> generatedQuestions;
        try {
            generatedQuestions = azureOpenAiPromptService.generateInterviewQuestions(
                    projectSession,
                    savedPreContext
            );
            auditLogService.recordSuccess(
                    request.getMemberId(),
                    sessionId,
                    "AI_INTERVIEW_QUESTION_GENERATE",
                    buildAiQuestionRequestSummary(projectSession, savedPreContext),
                    "questionCount=" + generatedQuestions.size()
            );
        } catch (RuntimeException e) {
            auditLogService.recordFailure(
                    request.getMemberId(),
                    sessionId,
                    "AI_INTERVIEW_QUESTION_GENERATE",
                    buildAiQuestionRequestSummary(projectSession, savedPreContext),
                    e.getMessage()
            );
            throw e;
        }

        List<InterviewQuestion> interviewQuestions = saveInterviewQuestions(projectSession, generatedQuestions);
        List<InterviewQuestionResponse> questionResponses = interviewQuestions.stream()
                .map(InterviewQuestionResponse::from)
                .toList();

        runAfterCommit(() -> auditLogService.recordSuccess(
                request.getMemberId(),
                sessionId,
                "PRE_CONTEXT_CREATE",
                buildPreContextRequestSummary(request),
                "contextId=%d, questionCount=%d".formatted(savedPreContext.getId(), questionResponses.size())
        ));

        return new PreContextResponse(savedPreContext.getId(), questionResponses);
    }

    // 생성된 질문 문자열 목록을 세션에 연결된 면접 질문 엔티티로 저장한다.
    private List<InterviewQuestion> saveInterviewQuestions(ProjectSession projectSession, List<String> questions) {
        List<InterviewQuestion> interviewQuestions = new ArrayList<>();

        for (int index = 0; index < questions.size(); index++) {
            interviewQuestions.add(new InterviewQuestion(projectSession, questions.get(index), index + 1));
        }

        return interviewQuestionRepository.saveAll(interviewQuestions);
    }

    // 현재 트랜잭션이 커밋된 뒤 후속 작업을 실행하도록 등록한다.
    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    // 요청 본문 자체가 누락되지 않았는지 검사한다.
    private void validateRequestBody(Object request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문은 필수입니다.");
        }
    }

    // 전달받은 필수 값들 중 null 이거나 빈 문자열이 없는지 검사한다.
    private void validateRequiredFields(String message, Object... values) {
        for (Object value : values) {
            if (value == null || value instanceof String stringValue && stringValue.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }
        }
    }

    // 프로젝트 세션 생성 요청을 감사 로그용 요약 문자열로 변환한다.
    private String buildProjectSessionRequestSummary(ProjectSessionRequest request) {
        return "memberId=%d, codeLength=%d, githubUrlProvided=%s".formatted(
                request.getMemberId(),
                lengthOf(request.getCodeContent()),
                request.getGithubUrl() != null && !request.getGithubUrl().isBlank()
        );
    }

    // 사전 컨텍스트 생성 요청을 감사 로그용 요약 문자열로 변환한다.
    private String buildPreContextRequestSummary(PreContextRequest request) {
        return "memberId=%d, codePurposeLength=%d, techRationaleLength=%d, exceptionHandlingLength=%d, projectScale=%s"
                .formatted(
                        request.getMemberId(),
                        lengthOf(request.getCodePurpose()),
                        lengthOf(request.getTechRationale()),
                        lengthOf(request.getExceptionHandling()),
                        request.getProjectScale()
                );
    }

    // AI 질문 생성 요청에 사용된 입력값을 감사 로그용 요약 문자열로 변환한다.
    private String buildAiQuestionRequestSummary(ProjectSession projectSession, PreContext preContext) {
        return "codeLength=%d, codePurposeLength=%d, techRationaleLength=%d, exceptionHandlingLength=%d, projectScale=%s"
                .formatted(
                        lengthOf(projectSession.getCodeContent()),
                        lengthOf(preContext.getCodePurpose()),
                        lengthOf(preContext.getTechRationale()),
                        lengthOf(preContext.getExceptionHandling()),
                        preContext.getProjectScale()
                );
    }

    // 문자열 길이를 반환하고 값이 없으면 0으로 처리한다.
    private int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }
}
