package com.example.trouble_log.domain.interview.repository;

import com.example.trouble_log.domain.interview.entity.InterviewAnswer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {

    Optional<InterviewAnswer> findByInterviewQuestionId(Long questionId);
}
