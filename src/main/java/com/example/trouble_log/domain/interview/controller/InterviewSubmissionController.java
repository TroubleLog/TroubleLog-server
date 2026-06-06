package com.example.trouble_log.domain.interview.controller;

import com.example.trouble_log.domain.interview.dto.InterviewSubmitRequest;
import com.example.trouble_log.domain.interview.dto.InterviewSubmitResponse;
import com.example.trouble_log.domain.interview.service.InterviewSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class InterviewSubmissionController {

    private final InterviewSubmissionService interviewSubmissionService;

    @PostMapping("/{sessionId}/interview/submit")
    public ResponseEntity<InterviewSubmitResponse> submit(
            @PathVariable Long sessionId,
            @RequestBody InterviewSubmitRequest request
    ) {
        InterviewSubmitResponse response = interviewSubmissionService.submit(sessionId, request);
        return ResponseEntity.ok(response);
    }
}
