package com.example.trouble_log.domain.projectSession.service;

import com.example.trouble_log.domain.audit.service.AuditLogService;
import com.example.trouble_log.domain.ai.service.AzureOpenAiPromptService;
import com.example.trouble_log.domain.interview.dto.InterviewQuestionResponse;
import com.example.trouble_log.domain.interview.entity.InterviewQuestion;
import com.example.trouble_log.domain.interview.repository.InterviewQuestionRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public ProjectSessionResponse create(ProjectSessionRequest request) {
        validateRequestBody(request);
        validateRequiredFields("회원 ID와 소스코드는 필수입니다.", request.getMemberId(), request.getCodeContent());

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> {
                    auditLogService.recordFailure(
                            request.getMemberId(),
                            null,
                            "PROJECT_SESSION_CREATE",
                            buildProjectSessionRequestSummary(request),
                            "유저를 찾지 못했습니다."
                    );
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾지 못했습니다.");
                });

        ProjectSession projectSession = new ProjectSession(
                member,
                request.getCodeContent(),
                request.getGithubUrl()
        );

        ProjectSession savedProjectSession = projectSessionRepository.save(projectSession);

        auditLogService.recordSuccess(
                member.getId(),
                savedProjectSession.getId(),
                "PROJECT_SESSION_CREATE",
                buildProjectSessionRequestSummary(request),
                "sessionId=" + savedProjectSession.getId()
        );

        return new ProjectSessionResponse(savedProjectSession.getId());
    }

    @Transactional
    public PreContextResponse createPreContext(Long sessionId, PreContextRequest request) {
        validateRequestBody(request);
        validateRequiredFields(
                "회원 ID, 사전 컨텍스트 답변은 필수입니다.",
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

        if (preContextRepository.existsByProjectSessionId(sessionId)) {
            auditLogService.recordFailure(
                    request.getMemberId(),
                    sessionId,
                    "PRE_CONTEXT_CREATE",
                    buildPreContextRequestSummary(request),
                    "이미 사전 컨텍스트가 저장된 프로젝트 세션입니다."
            );
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사전 컨텍스트가 저장된 프로젝트 세션입니다.");
        }

        PreContext preContext = new PreContext(
                projectSession,
                request.getCodePurpose(),
                request.getTechRationale(),
                request.getExceptionHandling(),
                request.getProjectScale()
        );

        PreContext savedPreContext = preContextRepository.save(preContext);

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

        auditLogService.recordSuccess(
                request.getMemberId(),
                sessionId,
                "PRE_CONTEXT_CREATE",
                buildPreContextRequestSummary(request),
                "contextId=%d, questionCount=%d".formatted(savedPreContext.getId(), questionResponses.size())
        );

        return new PreContextResponse(savedPreContext.getId(), questionResponses);
    }

    private List<InterviewQuestion> saveInterviewQuestions(ProjectSession projectSession, List<String> questions) {
        List<InterviewQuestion> interviewQuestions = new ArrayList<>();

        for (int index = 0; index < questions.size(); index++) {
            interviewQuestions.add(new InterviewQuestion(projectSession, questions.get(index), index + 1));
        }

        return interviewQuestionRepository.saveAll(interviewQuestions);
    }

    private void validateRequestBody(Object request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문은 필수입니다.");
        }
    }

    private void validateRequiredFields(String message, Object... values) {
        for (Object value : values) {
            if (value == null || value instanceof String stringValue && stringValue.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }
        }
    }

    private String buildProjectSessionRequestSummary(ProjectSessionRequest request) {
        return "memberId=%d, codeLength=%d, githubUrlProvided=%s".formatted(
                request.getMemberId(),
                lengthOf(request.getCodeContent()),
                request.getGithubUrl() != null && !request.getGithubUrl().isBlank()
        );
    }

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

    private int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }
}
