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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public InterviewAnswer(InterviewQuestion interviewQuestion, String answer) {
        this.interviewQuestion = interviewQuestion;
        this.answer = answer;
        this.createdAt = LocalDateTime.now();
    }

    public void updateAnswer(String answer) {
        this.answer = answer;
    }
}
