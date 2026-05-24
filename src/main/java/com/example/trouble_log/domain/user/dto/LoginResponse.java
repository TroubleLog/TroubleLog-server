package com.example.trouble_log.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private Long memberId;
    private String email;
    private String username;
}
