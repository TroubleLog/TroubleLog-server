package com.example.trouble_log.domain.projectSession.controller;

import com.example.trouble_log.domain.projectSession.dto.ProjectSessionRequest;
import com.example.trouble_log.domain.projectSession.dto.ProjectSessionResponse;
import com.example.trouble_log.domain.projectSession.service.ProjectSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectSessionController {

    private final ProjectSessionService projectSessionService;

    @PostMapping()
    public ResponseEntity<ProjectSessionResponse> create(@RequestBody ProjectSessionRequest request) {
        ProjectSessionResponse response = projectSessionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
