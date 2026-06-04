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

    private String loadPromptTemplate(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "프롬프트 템플릿을 읽지 못했습니다.", e);
        }
    }

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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public AnswerFeedbackResult evaluateAnswer(String question, String answer) {
        if (isBlank(question) || isBlank(answer)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문과 답변은 필수입니다.");
        }

        String userPrompt = loadPromptTemplate(ANSWER_FEEDBACK_USER_PROMPT_PATH)
                .replace("{question}", question)
                .replace("{answer}", answer);

        String response = generate(
                loadPromptTemplate(ANSWER_FEEDBACK_SYSTEM_PROMPT_PATH),
                userPrompt
        );

        return parseAnswerFeedbackResponse(response);
    }

    private AnswerFeedbackResult parseAnswerFeedbackResponse(String response) {
        String json = extractJsonObject(response);
        try {
            return objectMapper.readValue(json, AnswerFeedbackResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "답변 피드백 응답을 해석하지 못했습니다.", e);
        }
    }

    // 답변 피드백
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
    private CodeEvaluationResult parseCodeEvaluationResponse(String response) {
        String json = extractJsonObject(response);
        try {
            return objectMapper.readValue(json, CodeEvaluationResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "코드 평가 응답을 해석하지 못했습니다.", e);
        }
    }

    //JSON 객체 추출 헬퍼
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
