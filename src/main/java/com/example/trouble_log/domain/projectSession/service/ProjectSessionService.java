package com.example.trouble_log.domain.projectSession.service;

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

    @Transactional
    public ProjectSessionResponse create(ProjectSessionRequest request) {
        validateRequestBody(request);
        validateRequiredFields("회원 ID와 소스코드는 필수입니다.", request.getMemberId(), request.getCodeContent());

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾지 못했습니다."));

        ProjectSession projectSession = new ProjectSession(
                member,
                request.getCodeContent(),
                request.getGithubUrl()
        );

        ProjectSession savedProjectSession = projectSessionRepository.save(projectSession);

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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 프로젝트 세션에 접근할 수 없습니다.");
        }

        if (preContextRepository.existsByProjectSessionId(sessionId)) {
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

        List<String> generatedQuestions = azureOpenAiPromptService.generateInterviewQuestions(
                projectSession,
                savedPreContext
        );
        List<InterviewQuestion> interviewQuestions = saveInterviewQuestions(projectSession, generatedQuestions);
        List<InterviewQuestionResponse> questionResponses = interviewQuestions.stream()
                .map(InterviewQuestionResponse::from)
                .toList();

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
}
