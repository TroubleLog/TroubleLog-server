package com.example.trouble_log.domain.interview.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PersonalInfoType {

    EMAIL("이메일 주소로 보이는 정보가 포함되어 있습니다."),
    PHONE("전화번호로 보이는 정보가 포함되어 있습니다."),
    RESIDENT_REGISTRATION_NUMBER("주민등록번호로 보이는 정보가 포함되어 있습니다."),
    CREDIT_CARD("카드번호로 보이는 정보가 포함되어 있습니다."),
    PASSWORD("비밀번호로 보이는 정보가 포함되어 있습니다."),
    API_KEY("API Key 또는 토큰으로 보이는 정보가 포함되어 있습니다."),
    DATABASE_PASSWORD("DB 비밀번호로 보이는 정보가 포함되어 있습니다.");

    private final String warningMessage;
}
