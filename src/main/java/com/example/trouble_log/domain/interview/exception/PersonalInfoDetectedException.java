package com.example.trouble_log.domain.interview.exception;

import com.example.trouble_log.domain.interview.dto.PersonalInfoWarning;
import java.util.List;
import lombok.Getter;

@Getter
public class PersonalInfoDetectedException extends RuntimeException {

    private final List<PersonalInfoWarning> warnings;

    public PersonalInfoDetectedException(List<PersonalInfoWarning> warnings) {
        super("개인정보로 보이는 정보가 포함되어 있습니다.");
        this.warnings = warnings;
    }
}
