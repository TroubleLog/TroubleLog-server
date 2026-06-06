package com.example.trouble_log.domain.user.controller;

import com.example.trouble_log.domain.user.dto.LoginRequest;
import com.example.trouble_log.domain.user.dto.LoginResponse;
import com.example.trouble_log.domain.user.dto.SignUpRequest;
import com.example.trouble_log.domain.user.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
@Tag(name = "Member", description = "회원가입, 로그인, 로그아웃 API")
public class MemberController {

    private final MemberService memberService;

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호를 검증하고, 프론트에서 이후 API 호출에 사용할 회원 ID를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "이메일 또는 비밀번호 누락"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "로그인에 사용할 이메일과 비밀번호",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class))
            )
            @RequestBody LoginRequest request
    ) {
        LoginResponse response = memberService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "로그아웃",
            description = "현재는 서버 세션이나 토큰을 무효화하지 않고, 프론트 로그아웃 플로우를 위한 종료 응답만 반환합니다."
    )
    @ApiResponse(responseCode = "204", description = "로그아웃 처리 완료")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        memberService.logout();
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "회원가입",
            description = "신규 회원을 생성하고, 생성된 회원 정보를 로그인 응답 형태로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "이메일, 비밀번호 또는 사용자 이름 누락"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
    })
    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "회원가입에 필요한 이메일, 비밀번호, 사용자 이름",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SignUpRequest.class))
            )
            @RequestBody SignUpRequest request
    ) {
        LoginResponse response = memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
