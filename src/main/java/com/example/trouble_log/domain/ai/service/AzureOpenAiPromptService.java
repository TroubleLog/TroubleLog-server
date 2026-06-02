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

@Service
@RequiredArgsConstructor
public class AzureOpenAiPromptService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    private static final String INTERVIEW_QUESTION_SYSTEM_PROMPT_PATH = "prompts/interview-question-system.txt";
    private static final String INTERVIEW_QUESTION_USER_PROMPT_PATH = "prompts/interview-question-user.txt";

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

        String response = generate(
                loadPromptTemplate(INTERVIEW_QUESTION_SYSTEM_PROMPT_PATH),
                buildInterviewQuestionPrompt(projectSession, preContext)
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

    private String buildInterviewQuestionPrompt(ProjectSession projectSession, PreContext preContext) {
        return loadPromptTemplate(INTERVIEW_QUESTION_USER_PROMPT_PATH)
                .replace("{codeContent}", projectSession.getCodeContent())
                .replace("{codePurpose}", preContext.getCodePurpose())
                .replace("{techRationale}", preContext.getTechRationale())
                .replace("{exceptionHandling}", preContext.getExceptionHandling())
                .replace("{projectScale}", preContext.getProjectScale());
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
}
