package com.example.trouble_log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.example.trouble_log.domain.ai",
        "com.example.trouble_log.global.config"
})
public class AiCliApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCliApplication.class, args);
    }
}
