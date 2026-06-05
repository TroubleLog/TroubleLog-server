package com.example.trouble_log.domain.interview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "interview_answer")
public class InterviewAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private InterviewQuestion interviewQuestion;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false)
    private Boolean isSkipped = false;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // main 브랜치 생성자 유지
    public InterviewAnswer(InterviewQuestion interviewQuestion, String answer) {
        this.interviewQuestion = interviewQuestion;
        this.answer = answer;
        this.createdAt = LocalDateTime.now();
    }

    // [내 브랜치] 답변 저장용 생성자 추가
    public InterviewAnswer(InterviewQuestion interviewQuestion,
                           String answer,
                           Boolean isSkipped,
                           String feedback) {
        this.interviewQuestion = interviewQuestion;
        this.answer = answer;
        this.isSkipped = isSkipped;
        this.feedback = feedback;
        this.createdAt = LocalDateTime.now();
    }

    // main 브랜치 메서드 유지
    public void updateAnswer(String answer) {
        this.answer = answer;
    }
}
