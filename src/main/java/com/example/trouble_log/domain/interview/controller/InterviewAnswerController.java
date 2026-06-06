package com.example.trouble_log.domain.interview.controller;

import com.example.trouble_log.domain.interview.dto.InterviewAnswerFeedbackRequest;
import com.example.trouble_log.domain.interview.dto.InterviewAnswerFeedbackResponse;
import com.example.trouble_log.domain.interview.service.InterviewAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview/questions")
public class InterviewAnswerController {

    private final InterviewAnswerService interviewAnswerService;

    @PostMapping("/{questionId}/feedback")
    public ResponseEntity<InterviewAnswerFeedbackResponse> createFeedback(
            @PathVariable Long questionId,
            @RequestBody InterviewAnswerFeedbackRequest request
    ) {
        InterviewAnswerFeedbackResponse response = interviewAnswerService.createFeedback(questionId, request);
        return ResponseEntity.ok(response);
    }
}
