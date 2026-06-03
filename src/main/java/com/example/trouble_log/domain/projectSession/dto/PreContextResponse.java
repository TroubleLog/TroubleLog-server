package com.example.trouble_log.domain.projectSession.dto;

import com.example.trouble_log.domain.interview.dto.InterviewQuestionResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor @AllArgsConstructor
@Schema(description = "사전 컨텍스트 생성 응답")
public class PreContextResponse {
    @Schema(description = "컨텍스트 ID", example = "1")
    private Long contextId;

    @Schema(description = "Azure OpenAI가 생성한 면접 질문 목록")
    private List<InterviewQuestionResponse> questions;
}
