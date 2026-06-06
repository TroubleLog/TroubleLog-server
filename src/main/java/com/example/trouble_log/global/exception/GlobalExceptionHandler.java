package com.example.trouble_log.global.exception;

import com.example.trouble_log.domain.interview.dto.PersonalInfoDetectionResponse;
import com.example.trouble_log.domain.interview.exception.PersonalInfoDetectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PersonalInfoDetectedException.class)
    public ResponseEntity<PersonalInfoDetectionResponse> handlePersonalInfoDetected(PersonalInfoDetectedException e) {
        PersonalInfoDetectionResponse response = new PersonalInfoDetectionResponse(
                "PERSONAL_INFO_DETECTED",
                e.getWarnings()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
