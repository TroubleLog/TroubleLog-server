package com.example.trouble_log.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AzureOpenAiPromptService {

    private final ChatClient chatClient;

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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
