package com.example.trouble_log.domain.interview.service;

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
import com.example.trouble_log.domain.interview.repository.InterviewAnswerRepository;
import com.example.trouble_log.domain.interview.repository.InterviewQuestionRepository;
import com.example.trouble_log.domain.projectSession.entity.PreContext;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import com.example.trouble_log.domain.projectSession.repository.PreContextRepository;
import com.example.trouble_log.domain.projectSession.repository.ProjectSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
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
    private final RadarScoreCalculator radarCalculator;
    private final AnalysisResultRepository analysisResultRepository;

    // ── 답변 저장 + AI 피드백 생성 ───────────────────────────
    @Transactional
    public AnswerResponse saveAnswer(Long sessionId, Long questionId, AnswerRequest request) {
        // 세션 존재 확인
        ProjectSession session = sessionRepository.findById(sessionId)
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

        PreContext preContext = preContextRepository.findByProjectSession(session)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "사전 컨텍스트를 찾을 수 없습니다."));

        // 스킵 여부 판단
        boolean isSkipped = request.getAnswer() == null || request.getAnswer().isBlank();

        String feedbackJson = null;
        String improvement = null;
        String warning = null;

        if (!isSkipped) {
            // AI 피드백 생성
            AnswerFeedbackResult feedbackResult = promptService.evaluateAnswer(
                    session,
                    preContext,
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
    @Transactional
    public ReportResponse generateReport(Long sessionId) {
        // 세션 조회
        ProjectSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."));

        // 사전 컨텍스트 조회
        PreContext preContext = preContextRepository.findByProjectSession(session)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "사전 컨텍스트를 찾을 수 없습니다."));

        // 질문 + 답변 조회
        List<InterviewQuestion> questions = questionRepository
                .findByProjectSessionIdOrderByQuestionSequenceAsc(sessionId);

        // qaPairs 조립
        StringBuilder qaPairs = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            InterviewQuestion q = questions.get(i);
            InterviewAnswer a = q.getInterviewAnswer();

            qaPairs.append("Q").append(i + 1).append(". ")
                    .append(q.getQuestion()).append("\n");

            if (a == null || a.isSkipped()) {
                qaPairs.append("A").append(i + 1).append(". (스킵)\n\n");
            } else {
                qaPairs.append("A").append(i + 1).append(". ")
                        .append(a.getAnswer()).append("\n\n");
            }
        }

        // DB에서 feedback 파싱 (AI 재호출 없음)
        List<AnswerFeedbackResult> feedbackList = questions.stream()
                .filter(q -> q.getInterviewAnswer() != null
                        && !q.getInterviewAnswer().isSkipped()
                        && q.getInterviewAnswer().getFeedback() != null)
                .map(q -> {
                    try {
                        AnswerFeedbackResult result = objectMapper.readValue(
                                q.getInterviewAnswer().getFeedback(),
                                AnswerFeedbackResult.class);
                        // [추가] scores null 체크
                        if (result.getScores() == null) {
                            log.warn("피드백 scores 없음. questionId={}", q.getId());
                            return null;
                        }
                        return result;
                    } catch (Exception e) {
                        log.warn("피드백 파싱 실패. questionId={}", q.getId(), e);
                        return null;
                    }
                })
                .filter(f -> f != null)
                .toList();

        // 코드 평가
        CodeEvaluationResult codeEval = promptService.evaluateCode(session.getCodeContent());

        // 레이더 계산 (전체 답변 평균)
        RadarScore radarScore = null;
        if (!feedbackList.isEmpty()) {
            AnswerFeedbackResult avgFeedback = averageFeedback(feedbackList);
            if (avgFeedback != null) {                    // ← null 체크 추가
                radarScore = radarCalculator.calculate(avgFeedback, codeEval);
            }
        }

        // 리포트 생성
        String reportMarkdown = promptService.generateReport(
                session.getCodeContent(),
                preContext.getCodePurpose(),
                preContext.getTechRationale(),
                preContext.getExceptionHandling(),
                preContext.getProjectScale(),
                qaPairs.toString()
        );

        // 섹션 파싱
        String background = extractSection(reportMarkdown, "## Background", "## Problem");
        String problem    = extractSection(reportMarkdown, "## Problem", "## Root Cause");
        String cause      = extractSection(reportMarkdown, "## Root Cause", "## Resolution");
        String solution   = extractSection(reportMarkdown, "## Resolution", "## Result");
        String result     = extractSection(reportMarkdown, "## Result", null);

        // DB 저장
        if (radarScore != null) {
            AnalysisResult analysisResult = new AnalysisResult(
                    session, radarScore, codeEval,
                    background, problem, cause, solution, result
            );
            analysisResultRepository.save(analysisResult);
        }

        return new ReportResponse(reportMarkdown, radarScore);
    }

    // 전체 답변 피드백 평균 계산
    private AnswerFeedbackResult averageFeedback(List<AnswerFeedbackResult> feedbackList) {
        // scores null 방어
        List<AnswerFeedbackResult> validList = feedbackList.stream()
                .filter(f -> f.getScores() != null)
                .toList();

        if (validList.isEmpty()) return null;

        int specificity = (int) Math.round(validList.stream()
                .mapToInt(f -> f.getScores().getSpecificity()).average().orElse(0));
        int structure = (int) Math.round(validList.stream()
                .mapToInt(f -> f.getScores().getStructure()).average().orElse(0));
        int relevance = (int) Math.round(validList.stream()
                .mapToInt(f -> f.getScores().getRelevance()).average().orElse(0));
        int keyword = (int) Math.round(validList.stream()
                .mapToInt(f -> f.getScores().getKeyword()).average().orElse(0));

        AnswerFeedbackResult.Scores avgScores = new AnswerFeedbackResult.Scores();
        avgScores.setSpecificity(specificity);
        avgScores.setStructure(structure);
        avgScores.setRelevance(relevance);
        avgScores.setKeyword(keyword);

        AnswerFeedbackResult avg = new AnswerFeedbackResult();
        avg.setScores(avgScores);
        return avg;
    }

    // 섹션 추출 헬퍼
    private String extractSection(String markdown, String startHeader, String endHeader) {
        int start = markdown.indexOf(startHeader);
        if (start < 0) return "";
        start += startHeader.length();

        int end = endHeader != null ? markdown.indexOf(endHeader, start) : markdown.length();
        if (end < 0) end = markdown.length();

        return markdown.substring(start, end).trim();
    }
}
