package com.example.trouble_log.domain.interview.repository;

import com.example.trouble_log.domain.interview.entity.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {

    // main 브랜치 메서드 유지
    Optional<InterviewAnswer> findByInterviewQuestionId(Long questionId);
}
