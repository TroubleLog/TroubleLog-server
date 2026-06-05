package com.example.trouble_log.domain.interview.service;

import com.example.trouble_log.domain.interview.dto.PersonalInfoWarning;
import com.example.trouble_log.domain.interview.type.PersonalInfoType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class PersonalInfoDetectionService {

    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
    );
    private static final Pattern PHONE = Pattern.compile(
            "(?<!\\d)(?:01[016789][-\\s]?\\d{3,4}[-\\s]?\\d{4}|0\\d{1,2}[-\\s]?\\d{3,4}[-\\s]?\\d{4})(?!\\d)"
    );
    private static final Pattern RESIDENT_REGISTRATION_NUMBER = Pattern.compile(
            "(?<!\\d)\\d{6}[-\\s]?[1-4]\\d{6}(?!\\d)"
    );
    private static final Pattern CREDIT_CARD = Pattern.compile(
            "(?<!\\d)(?:\\d{4}[-\\s]?){3}\\d{4}(?!\\d)"
    );
    private static final Pattern PASSWORD = Pattern.compile(
            "(?i)(?:password|passwd|pwd|비밀번호)\\s*[:=]\\s*['\"]?[^\\s'\"]{6,}"
    );
    private static final Pattern API_KEY = Pattern.compile(
            "(?i)(?:api[_-]?key|access[_-]?token|secret[_-]?key|bearer)\\s*[:=]\\s*['\"]?[A-Za-z0-9_./+=-]{8,}"
    );
    private static final Pattern DATABASE_PASSWORD = Pattern.compile(
            "(?i)(?:db[_-]?password|database[_-]?password|spring\\.datasource\\.password)\\s*[:=]\\s*['\"]?[^\\s'\"]{4,}"
    );

    // 전달된 텍스트에서 개인정보나 민감정보로 보이는 패턴을 찾아 경고 목록으로 변환한다.
    public List<PersonalInfoWarning> detect(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Map<PersonalInfoType, String> warnings = new LinkedHashMap<>();
        addIfMatches(warnings, PersonalInfoType.EMAIL, EMAIL, text);
        addIfMatches(warnings, PersonalInfoType.PHONE, PHONE, text);
        addIfMatches(
                warnings,
                PersonalInfoType.RESIDENT_REGISTRATION_NUMBER,
                RESIDENT_REGISTRATION_NUMBER,
                text
        );
        addIfMatches(warnings, PersonalInfoType.CREDIT_CARD, CREDIT_CARD, text);
        addIfMatches(warnings, PersonalInfoType.PASSWORD, PASSWORD, text);
        addIfMatches(warnings, PersonalInfoType.API_KEY, API_KEY, text);
        addIfMatches(warnings, PersonalInfoType.DATABASE_PASSWORD, DATABASE_PASSWORD, text);

        return warnings.entrySet().stream()
                .map(entry -> new PersonalInfoWarning(entry.getKey(), entry.getValue()))
                .toList();
    }

    // 특정 패턴이 감지되면 같은 타입의 경고가 중복 추가되지 않도록 타입별로 한 번만 기록한다.
    private void addIfMatches(
            Map<PersonalInfoType, String> warnings,
            PersonalInfoType type,
            Pattern pattern,
            String text
    ) {
        if (pattern.matcher(text).find()) {
            warnings.put(type, type.getWarningMessage());
        }
    }
}
