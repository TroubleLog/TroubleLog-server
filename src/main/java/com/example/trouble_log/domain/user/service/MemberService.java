package com.example.trouble_log.domain.user.service;

import com.example.trouble_log.domain.audit.service.AuditLogService;
import com.example.trouble_log.domain.user.dto.LoginRequest;
import com.example.trouble_log.domain.user.dto.LoginResponse;
import com.example.trouble_log.domain.user.dto.SignUpRequest;
import com.example.trouble_log.domain.user.entity.Member;
import com.example.trouble_log.domain.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;

     // 이메일과 비밀번호를 검증해 로그인 가능한 회원 정보를 반환한다.

    public LoginResponse login(LoginRequest request) {
        validateLoginRequest(request);

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    auditLogService.recordFailure(
                            null,
                            null,
                            "MEMBER_LOGIN",
                            "email=" + maskEmail(request.getEmail()),
                            "유효하지 않은 이메일 혹은 비밀번호입니다."
                    );
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 이메일 혹은 비밀번호입니다.");
                });

        if (!member.getPassword().equals(request.getPassword())) {
            auditLogService.recordFailure(
                    member.getId(),
                    null,
                    "MEMBER_LOGIN",
                    "email=" + maskEmail(request.getEmail()),
                    "유효하지 않은 이메일 혹은 비밀번호입니다."
            );
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 이메일 혹은 비밀번호입니다.");
        }

        auditLogService.recordSuccess(
                member.getId(),
                null,
                "MEMBER_LOGIN",
                "email=" + maskEmail(request.getEmail()),
                "login success"
        );

        return new LoginResponse(member.getId(), member.getEmail(), member.getUsername());
    }

    // 로그인 요청 본문과 필수 입력값이 모두 존재하는지 검사한다.
    private void validateLoginRequest(LoginRequest request) {
        if (request == null || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일과 패스워드는 필수입니다.");
        }
    }

    // 함수에 공백이나 null을 넣으면 true라고 반환, NPE 대비
    // 문자열이 null 이거나 공백만 포함하는지 확인한다.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // 현재는 별도 상태 변경 없이 로그아웃 호출 지점을 위한 빈 메서드를 제공한다.
    public void logout() {
    }

    /**
     * 회원가입 요청을 검증하고 새 회원을 저장한 뒤 로그인 응답 형태로 반환한다.
     */
    @Transactional
    public LoginResponse signup(SignUpRequest request) {
        validateSignUpRequest(request);

        // 이미 존재하는 이메일일 경우
        if (memberRepository.existsByEmail(request.getEmail())) {
            auditLogService.recordFailure(
                    null,
                    null,
                    "MEMBER_SIGNUP",
                    "email=" + maskEmail(request.getEmail()),
                    "이미 사용 중인 이메일입니다."
            );
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        Member member = new Member(
                request.getEmail(),
                request.getPassword(),
                request.getUsername()
        );

        Member savedMember = memberRepository.save(member);

        auditLogService.recordSuccess(
                savedMember.getId(),
                null,
                "MEMBER_SIGNUP",
                "email=" + maskEmail(savedMember.getEmail()),
                "memberId=" + savedMember.getId()
        );

        return new LoginResponse(savedMember.getId(), savedMember.getEmail(), savedMember.getUsername());
    }

    /**
     * 회원가입 요청 본문과 필수 입력값이 모두 존재하는지 검사한다.
     */
    private void validateSignUpRequest(SignUpRequest request) {
        if (request == null
                || isBlank(request.getEmail())
                || isBlank(request.getPassword())
                || isBlank(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일, 패스워드, 이름은 필수입니다.");
        }
    }

    /**
     * 감사 로그에 남길 때 개인정보 노출을 줄이기 위해 이메일 일부를 마스킹한다.
     */
    private String maskEmail(String email) {
        if (isBlank(email) || !email.contains("@")) {
            return "unknown";
        }

        String[] parts = email.split("@", 2);
        String name = parts[0];
        String domain = parts[1];

        if (isBlank(name) || isBlank(domain)) {
            return "unknown";
        }

        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + domain;
        }

        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }
}
