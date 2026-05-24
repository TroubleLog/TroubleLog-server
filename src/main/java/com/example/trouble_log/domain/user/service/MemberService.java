package com.example.trouble_log.domain.user.service;

import com.example.trouble_log.domain.user.dto.LoginRequest;
import com.example.trouble_log.domain.user.dto.LoginResponse;
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

    public LoginResponse login(LoginRequest request) {
        validateLoginRequest(request);

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 이메일 혹은 비밀번호입니다."));

        if (!member.getPassword().equals(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 이메일 혹은 비밀번호입니다.");
        }

        return new LoginResponse(member.getId(), member.getEmail(), member.getUsername());
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일과 패스워드는 필수입니다.");
        }
    }

    // 함수에 공백이나 null을 넣으면 true라고 반환, NPE 대비
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public void logout() {
    }
}
