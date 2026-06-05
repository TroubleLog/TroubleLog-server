package com.example.trouble_log.domain.interview.service;

import com.example.trouble_log.domain.ai.dto.AnswerFeedbackResult;
import com.example.trouble_log.domain.ai.service.AzureOpenAiPromptService;
import com.example.trouble_log.domain.interview.dto.AnswerRequest;
import com.example.trouble_log.domain.interview.dto.AnswerResponse;
import com.example.trouble_log.domain.interview.dto.ReportResponse;
import com.example.trouble_log.domain.interview.entity.InterviewAnswer;
import com.example.trouble_log.domain.interview.entity.InterviewQuestion;
import com.example.trouble_log.domain.interview.repository.InterviewAnswerRepository;
import com.example.trouble_log.domain.interview.repository.InterviewQuestionRepository;
import com.example.trouble_log.domain.projectSession.entity.PreContext;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import com.example.trouble_log.domain.projectSession.repository.PreContextRepository;
import com.example.trouble_log.domain.projectSession.repository.ProjectSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final ProjectSessionRepository sessionRepository;
    private final PreContextRepository preContextRepository;
    private final AzureOpenAiPromptService promptService;
    private final ObjectMapper objectMapper;

    // ── 답변 저장 + AI 피드백 생성 ───────────────────────────
    @Transactional
    public AnswerResponse saveAnswer(Long sessionId, Long questionId, AnswerRequest request) {
        // 세션 존재 확인
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."));

        // 질문 존재 확인
        InterviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."));

        // 세션 소유 확인
        if (!question.getProjectSession().getId().equals(sessionId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "해당 세션의 질문이 아닙니다.");
        }

        // 스킵 여부 판단
        boolean isSkipped = request.getAnswer() == null || request.getAnswer().isBlank();

        String feedbackJson = null;
        String improvement = null;
        String warning = null;

        if (!isSkipped) {
            // AI 피드백 생성
            AnswerFeedbackResult feedbackResult = promptService.evaluateAnswer(
                    question.getQuestion(),
                    request.getAnswer()
            );

            try {
                feedbackJson = objectMapper.writeValueAsString(feedbackResult);
            } catch (Exception e) {
                feedbackJson = "{}";
            }

            improvement = feedbackResult.getImprovement();
            warning = feedbackResult.getWarning();
        }

        InterviewAnswer interviewAnswer = new InterviewAnswer(
                question,
                isSkipped ? null : request.getAnswer(),
                isSkipped,
                feedbackJson
        );

        InterviewAnswer saved = answerRepository.save(interviewAnswer);
        return new AnswerResponse(saved.getId(), improvement, warning);
    }

    // ── 트러블슈팅 리포트 생성 ────────────────────────────────
    public ReportResponse generateReport(Long sessionId) {
        // 세션 조회
        ProjectSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."));

        // 사전 컨텍스트 조회
        PreContext preContext = preContextRepository.findByProjectSession(session)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "사전 컨텍스트를 찾을 수 없습니다."));

        // 질문 + 답변 조회 후 qaPairs 조립
        List<InterviewQuestion> questions = questionRepository
                .findByProjectSessionIdOrderByQuestionSequenceAsc(sessionId);

        StringBuilder qaPairs = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            InterviewQuestion q = questions.get(i);
            InterviewAnswer a = q.getInterviewAnswer();

            qaPairs.append("Q").append(i + 1).append(". ")
                    .append(q.getQuestion()).append("\n");

            if (a == null || a.getIsSkipped()) {
                qaPairs.append("A").append(i + 1).append(". (스킵)\n\n");
            } else {
                qaPairs.append("A").append(i + 1).append(". ")
                        .append(a.getAnswer()).append("\n\n");
            }
        }

        // AI 리포트 생성
        String report = promptService.generateReport(
                session.getCodeContent(),
                preContext.getCodePurpose(),
                preContext.getTechRationale(),
                preContext.getExceptionHandling(),
                preContext.getProjectScale(),
                qaPairs.toString()
        );

        return new ReportResponse(report);
    }
}
