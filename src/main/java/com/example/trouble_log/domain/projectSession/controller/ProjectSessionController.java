package com.example.trouble_log.domain.projectSession.controller;

import com.example.trouble_log.domain.interview.dto.PersonalInfoDetectionResponse;
import com.example.trouble_log.domain.projectSession.dto.PreContextRequest;
import com.example.trouble_log.domain.projectSession.dto.PreContextResponse;
import com.example.trouble_log.domain.projectSession.dto.ProjectSessionRequest;
import com.example.trouble_log.domain.projectSession.dto.ProjectSessionResponse;
import com.example.trouble_log.domain.projectSession.service.ProjectSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
@Tag(name = "Project Session", description = "프로젝트 코드 입력, 사전 컨텍스트 저장, AI 면접 질문 생성 API")
public class ProjectSessionController {

    private final ProjectSessionService projectSessionService;

    @Operation(
            summary = "프로젝트 세션 생성",
            description = "회원이 입력한 소스코드에서 개인정보와 민감정보를 먼저 감지합니다. 문제가 없으면 소스코드와 선택적인 GitHub URL을 저장하고, 이후 사전 컨텍스트와 면접 질문 생성에 사용할 세션 ID를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "프로젝트 세션 생성 성공",
                    content = @Content(schema = @Schema(implementation = ProjectSessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "회원 ID 또는 소스코드 누락, 개인정보 또는 민감정보 감지",
                    content = @Content(schema = @Schema(implementation = PersonalInfoDetectionResponse.class))),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @PostMapping()
    public ResponseEntity<ProjectSessionResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "프로젝트 세션 생성을 위한 회원 ID, 소스코드, GitHub URL",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ProjectSessionRequest.class))
            )
            @RequestBody ProjectSessionRequest request
    ) {
        ProjectSessionResponse response = projectSessionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "사전 컨텍스트 저장 및 면접 질문 생성",
            description = "프로젝트 목적, 기술 선택 이유, 예외 처리 방식, 프로젝트 규모를 저장한 뒤 Azure OpenAI로 면접 질문 3개를 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "사전 컨텍스트 저장 및 질문 생성 성공",
                    content = @Content(schema = @Schema(implementation = PreContextResponse.class))),
            @ApiResponse(responseCode = "400", description = "필수 사전 컨텍스트 값 누락"),
            @ApiResponse(responseCode = "403", description = "해당 프로젝트 세션에 접근할 수 없음"),
            @ApiResponse(responseCode = "404", description = "프로젝트 세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 사전 컨텍스트가 저장된 세션")
    })
    @PostMapping("/{sessionId}/pre-context")
    public ResponseEntity<PreContextResponse> createPreContext(
            @Parameter(description = "프로젝트 세션 ID", example = "1", required = true)
            @PathVariable Long sessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "면접 질문 생성을 위한 사전 컨텍스트",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PreContextRequest.class))
            )
            @RequestBody PreContextRequest request
    ) {
        PreContextResponse response = projectSessionService.createPreContext(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
