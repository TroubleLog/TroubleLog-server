package com.example.trouble_log.domain.interview.repository;

import com.example.trouble_log.domain.interview.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
}
