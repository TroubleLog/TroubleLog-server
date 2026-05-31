package com.example.trouble_log.domain.ai.cli;

import com.example.trouble_log.domain.ai.service.AzureOpenAiPromptService;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ai-cli")
@RequiredArgsConstructor
public class AzureOpenAiCliRunner implements CommandLineRunner {

    private final AzureOpenAiPromptService azureOpenAiPromptService;

    @Value("${app.ai-cli.system-prompt:}")
    private String systemPrompt;

    @Value("${app.ai-cli.user-prompt:}")
    private String userPrompt;

    @Value("${spring.ai.azure.openai.endpoint:}")
    private String endpoint;

    @Value("${spring.ai.azure.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.azure.openai.chat.options.deployment-name:}")
    private String deploymentName;

    @Override
    public void run(String... args) {
        if (!hasRequiredAzureOpenAiConfig()) {
            return;
        }

        String prompt = resolvePrompt(args);

        if (isBlank(prompt)) {
            System.err.println("질문이 비어 있어 실행을 종료합니다.");
            return;
        }

        String content = azureOpenAiPromptService.generate(systemPrompt, prompt);
        System.out.println(content);
    }

    private boolean hasRequiredAzureOpenAiConfig() {
        if (isBlank(endpoint) || endpoint.contains("${") || endpoint.contains("example.openai.azure.com")) {
            System.err.println("AZURE_OPENAI_ENDPOINT 환경변수가 설정되지 않았습니다.");
            return false;
        }
        if (isBlank(apiKey) || apiKey.contains("${") || "local-placeholder".equals(apiKey)) {
            System.err.println("AZURE_OPENAI_API_KEY 또는 AZURE_OPENAI_KEY 환경변수가 설정되지 않았습니다.");
            return false;
        }
        if (isBlank(deploymentName) || deploymentName.contains("${")) {
            System.err.println("AZURE_OPENAI_DEPLOYMENT_NAME 환경변수가 설정되지 않았습니다.");
            return false;
        }
        return true;
    }

    private String resolvePrompt(String[] args) {
        String promptFromArgs = Arrays.stream(args)
                .filter(arg -> !arg.startsWith("--"))
                .reduce((left, right) -> left + " " + right)
                .orElse("");

        if (!isBlank(promptFromArgs)) {
            return promptFromArgs;
        }

        if (!isBlank(userPrompt)) {
            return userPrompt;
        }

        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
