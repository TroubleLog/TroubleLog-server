package com.example.trouble_log.domain.interview.entity;

import com.example.trouble_log.domain.projectsession.entity.ProjectSession;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "interview_question")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ProjectSession projectSession;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @OneToOne(mappedBy = "interviewQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private InterviewAnswer interviewAnswer;

}
