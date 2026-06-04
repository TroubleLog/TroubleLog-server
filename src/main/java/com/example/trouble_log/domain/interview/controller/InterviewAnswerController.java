package com.example.trouble_log.domain.interview.controller;

import com.example.trouble_log.domain.interview.dto.InterviewAnswerRequest;
import com.example.trouble_log.domain.interview.dto.InterviewAnswerResponse;
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

    @PostMapping("/{questionId}/answer")
    public ResponseEntity<InterviewAnswerResponse> saveAnswer(
            @PathVariable Long questionId,
            @RequestBody InterviewAnswerRequest request
    ) {
        InterviewAnswerResponse response = interviewAnswerService.saveAnswer(questionId, request);
        return ResponseEntity.ok(response);
    }
}
