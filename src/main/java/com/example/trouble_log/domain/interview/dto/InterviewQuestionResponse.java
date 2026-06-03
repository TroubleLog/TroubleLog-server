package com.example.trouble_log.domain.interview.dto;

import com.example.trouble_log.domain.interview.entity.InterviewQuestion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "면접 질문 응답")
public class InterviewQuestionResponse {

    @Schema(description = "질문 ID", example = "1")
    private Long questionId;

    @Schema(description = "질문 순서", example = "1")
    private Integer questionSequence;

    @Schema(description = "면접 질문", example = "Spring Boot와 JPA를 선택한 이유는 무엇인가요?")
    private String question;

    public static InterviewQuestionResponse from(InterviewQuestion interviewQuestion) {
        return new InterviewQuestionResponse(
                interviewQuestion.getId(),
                interviewQuestion.getQuestionSequence(),
                interviewQuestion.getQuestion()
        );
    }
}
