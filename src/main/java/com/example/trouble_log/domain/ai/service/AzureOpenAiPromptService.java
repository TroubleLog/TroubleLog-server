package com.example.trouble_log.domain.ai.service;

import com.example.trouble_log.domain.projectSession.entity.PreContext;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.example.trouble_log.domain.ai.dto.AnswerFeedbackResult;
import com.example.trouble_log.domain.ai.dto.CodeEvaluationResult;

@Service
@RequiredArgsConstructor
public class AzureOpenAiPromptService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    private static final String INTERVIEW_QUESTION_SYSTEM_PROMPT_PATH = "prompts/interview-question-system.txt";
    private static final String INTERVIEW_QUESTION_USER_PROMPT_PATH = "prompts/interview-question-user.txt";
    private static final String ANSWER_FEEDBACK_SYSTEM_PROMPT_PATH = "prompts/answer-feedback-system.txt";
    private static final String ANSWER_FEEDBACK_USER_PROMPT_PATH   = "prompts/answer-feedback-user.txt";
    private static final String CODE_EVALUATION_SYSTEM_PROMPT_PATH = "prompts/code-evaluation-system.txt";
    private static final String CODE_EVALUATION_USER_PROMPT_PATH   = "prompts/code-evaluation-user.txt";

    // 시스템 프롬프트와 사용자 프롬프트를 기반으로 Azure OpenAI에 요청을 보내고 원문 응답을 반환한다.
    public String generate(String systemPrompt, String userPrompt) {
        if (isBlank(userPrompt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userPrompt는 필수입니다.");
        }

        ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt();
        if (!isBlank(systemPrompt)) {
            requestSpec = requestSpec.system(systemPrompt);
        }

        return requestSpec
                .user(userPrompt)
                .call()
                .content();
    }

    // 프로젝트 세션과 사전 컨텍스트를 이용해 면접 질문 3개를 생성한다.
    public List<String> generateInterviewQuestions(ProjectSession projectSession, PreContext preContext) {
        validateInterviewQuestionSource(projectSession, preContext);

        return generateInterviewQuestions(
                projectSession.getCodeContent(),
                preContext.getCodePurpose(),
                preContext.getTechRationale(),
                preContext.getExceptionHandling(),
                preContext.getProjectScale()
        );
    }

    // 코드와 사전 컨텍스트 문자열을 직접 받아 면접 질문 3개를 생성한다.
    public List<String> generateInterviewQuestions(
            String codeContent,
            String codePurpose,
            String techRationale,
            String exceptionHandling,
            String projectScale
    ) {
        validateInterviewQuestionSource(
                codeContent,
                codePurpose,
                techRationale,
                exceptionHandling,
                projectScale
        );

        String response = generate(
                loadPromptTemplate(INTERVIEW_QUESTION_SYSTEM_PROMPT_PATH),
                buildInterviewQuestionPrompt(
                        codeContent,
                        codePurpose,
                        techRationale,
                        exceptionHandling,
                        projectScale
                )
        );

        return parseQuestionResponse(response);
    }

    // 면접 질문 생성에 필요한 프로젝트 세션과 사전 컨텍스트가 모두 준비되었는지 검증한다.
    private void validateInterviewQuestionSource(ProjectSession projectSession, PreContext preContext) {
        if (projectSession == null || preContext == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로젝트 세션과 사전 컨텍스트는 필수입니다.");
        }

        if (isBlank(projectSession.getCodeContent())
                || isBlank(preContext.getCodePurpose())
                || isBlank(preContext.getTechRationale())
                || isBlank(preContext.getExceptionHandling())
                || isBlank(preContext.getProjectScale())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문 생성에 필요한 입력값이 비어 있습니다.");
        }
    }

    // 면접 질문 생성에 필요한 입력 문자열이 모두 비어 있지 않은지 검증한다.
    private void validateInterviewQuestionSource(
            String codeContent,
            String codePurpose,
            String techRationale,
            String exceptionHandling,
            String projectScale
    ) {
        if (isBlank(codeContent)
                || isBlank(codePurpose)
                || isBlank(techRationale)
                || isBlank(exceptionHandling)
                || isBlank(projectScale)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문 생성에 필요한 입력값이 비어 있습니다.");
        }
    }

    // 질문 생성용 사용자 프롬프트 템플릿에 코드와 컨텍스트 값을 주입한다.
    private String buildInterviewQuestionPrompt(
            String codeContent,
            String codePurpose,
            String techRationale,
            String exceptionHandling,
            String projectScale
    ) {
        return loadPromptTemplate(INTERVIEW_QUESTION_USER_PROMPT_PATH)
                .replace("{codeContent}", codeContent)
                .replace("{codePurpose}", codePurpose)
                .replace("{techRationale}", techRationale)
                .replace("{exceptionHandling}", exceptionHandling)
                .replace("{projectScale}", projectScale);
    }

    // 클래스패스에 있는 프롬프트 템플릿 파일을 UTF-8 문자열로 읽어온다.
    private String loadPromptTemplate(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "프롬프트 템플릿을 읽지 못했습니다.", e);
        }
    }

    // 질문 생성 응답에서 JSON 배열을 추출하고 질문 목록 형태로 변환한다.
    private List<String> parseQuestionResponse(String response) {
        String json = extractJsonArray(response);

        try {
            List<String> questions = objectMapper.readValue(json, new TypeReference<>() {
            });

            if (questions.size() != 3 || questions.stream().anyMatch(this::isBlank)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Azure OpenAI 질문 생성 응답 형식이 올바르지 않습니다.");
            }

            return questions;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Azure OpenAI 질문 생성 응답을 해석하지 못했습니다.", e);
        }
    }

    // OpenAI 응답 문자열에서 질문 목록에 해당하는 JSON 배열 구간만 추출한다.
    private String extractJsonArray(String response) {
        if (isBlank(response)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Azure OpenAI 질문 생성 응답이 비어 있습니다.");
        }

        int startIndex = response.indexOf('[');
        int endIndex = response.lastIndexOf(']');

        if (startIndex < 0 || endIndex < startIndex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Azure OpenAI 질문 생성 응답에 JSON 배열이 없습니다.");
        }

        return response.substring(startIndex, endIndex + 1);
    }

    // 문자열이 null 이거나 공백만 포함하는지 확인한다.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // AI가 반환한 점수가 허용 범위를 벗어나지 않았는지 검증한다.
    private void validateScore(String field, int score, int min, int max) {
        if (score < min || score > max) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    String.format("AI 응답의 %s 점수가 허용 범위(%d~%d)를 벗어났습니다: %d", field, min, max, score)
            );
        }
    }

    // 면접 질문과 답변을 기반으로 답변 피드백과 점수를 생성한다.
    public AnswerFeedbackResult evaluateAnswer(
            ProjectSession projectSession,
            PreContext preContext,
            String question,
            String answer
    ) {
        validateAnswerFeedbackSource(projectSession, preContext, question, answer);

        String userPrompt = loadPromptTemplate(ANSWER_FEEDBACK_USER_PROMPT_PATH)
                .replace("{codeContent}", projectSession.getCodeContent())
                .replace("{codePurpose}", preContext.getCodePurpose())
                .replace("{techRationale}", preContext.getTechRationale())
                .replace("{exceptionHandling}", preContext.getExceptionHandling())
                .replace("{projectScale}", preContext.getProjectScale())
                .replace("{question}", question)
                .replace("{answer}", answer);

        String response = generate(
                loadPromptTemplate(ANSWER_FEEDBACK_SYSTEM_PROMPT_PATH),
                userPrompt
        );

        return parseAnswerFeedbackResponse(response);
    }

    // 답변 피드백 생성에 필요한 프로젝트 세션, 사전 컨텍스트, 질문, 답변이 모두 준비되었는지 검증한다.
    private void validateAnswerFeedbackSource(
            ProjectSession projectSession,
            PreContext preContext,
            String question,
            String answer
    ) {
        if (projectSession == null || preContext == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로젝트 세션과 사전 컨텍스트는 필수입니다.");
        }

        if (isBlank(projectSession.getCodeContent())
                || isBlank(preContext.getCodePurpose())
                || isBlank(preContext.getTechRationale())
                || isBlank(preContext.getExceptionHandling())
                || isBlank(preContext.getProjectScale())
                || isBlank(question)
                || isBlank(answer)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "답변 피드백 생성에 필요한 입력값이 비어 있습니다.");
        }
    }

    // 답변 피드백 응답 JSON을 DTO로 변환하고 점수 형식을 검증한다.
    private AnswerFeedbackResult parseAnswerFeedbackResponse(String response) {
        String json = extractJsonObject(response);
        try {
            AnswerFeedbackResult result = objectMapper.readValue(json, AnswerFeedbackResult.class);

            if (result.getScores() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "답변 피드백 scores 필드가 없습니다.");
            }
            validateScore("specificity", result.getScores().getSpecificity(), 1, 5);
            validateScore("structure",   result.getScores().getStructure(),   1, 5);
            validateScore("relevance",   result.getScores().getRelevance(),   1, 5);
            validateScore("keyword",     result.getScores().getKeyword(),     1, 5);

            return result;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "답변 피드백 응답을 해석하지 못했습니다.", e);
        }
    }

    // 답변 피드백
    // 코드 내용을 기반으로 코드 평가 점수와 코멘트를 생성한다.
    public CodeEvaluationResult evaluateCode(String codeContent) {
        if (isBlank(codeContent)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "코드는 필수입니다.");
        }

        String userPrompt = loadPromptTemplate(CODE_EVALUATION_USER_PROMPT_PATH)
                .replace("{codeContent}", codeContent);

        String response = generate(
                loadPromptTemplate(CODE_EVALUATION_SYSTEM_PROMPT_PATH),
                userPrompt
        );

        return parseCodeEvaluationResponse(response);
    }

    // 코드 정량 평가
    // 코드 평가 응답 JSON을 DTO로 변환하고 각 항목 점수 범위를 검증한다.
    private CodeEvaluationResult parseCodeEvaluationResponse(String response) {
        String json = extractJsonObject(response);
        try {
            CodeEvaluationResult result = objectMapper.readValue(json, CodeEvaluationResult.class);

            if (result.getNaming() == null
                    || result.getSingleResponsibility() == null
                    || result.getErrorHandling() == null
                    || result.getDuplication() == null
                    || result.getCommentQuality() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "코드 평가 응답에 누락된 축이 있습니다.");
            }
            validateScore("naming",               result.getNaming().getScore(),               0, 20);
            validateScore("singleResponsibility", result.getSingleResponsibility().getScore(), 0, 20);
            validateScore("errorHandling",        result.getErrorHandling().getScore(),        0, 20);
            validateScore("duplication",          result.getDuplication().getScore(),          0, 20);
            validateScore("commentQuality",       result.getCommentQuality().getScore(),       0, 20);

            return result;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "코드 평가 응답을 해석하지 못했습니다.", e);
        }
    }

    //JSON 객체 추출 헬퍼
    // OpenAI 응답 문자열에서 JSON 객체 구간만 추출한다.
    private String extractJsonObject(String response) {
        if (isBlank(response)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Azure OpenAI 응답이 비어 있습니다.");
        }

        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');

        if (startIndex < 0 || endIndex < startIndex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Azure OpenAI 응답에 JSON 객체가 없습니다.");
        }

        return response.substring(startIndex, endIndex + 1);
    }
}
