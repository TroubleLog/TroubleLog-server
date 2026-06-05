package com.example.trouble_log.domain.interview.repository;

import com.example.trouble_log.domain.interview.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    // 세션별 질문 순서대로 조회 -> 리포트 생성 시 qaPairs 조립에 사용
    List<InterviewQuestion> findByProjectSessionIdOrderByQuestionSequenceAsc(Long sessionId);
}
