package com.example.trouble_log.domain.projectsession.entity;

import com.example.trouble_log.domain.user.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "project_session")
public class ProjectSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Lob
    @Column(name = "code_content", columnDefinition = "TEXT")
    private String codeContent;

    @Column(name = "github_url", length = 255)
    private String githubUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "projectSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private PreContext preContext;

}
