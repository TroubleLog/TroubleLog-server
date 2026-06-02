package com.example.trouble_log.domain.projectSession.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "pre_context")
public class PreContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private ProjectSession projectSession;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String codePurpose;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String techRationale;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String exceptionHandling;

    @Column(length = 100, nullable = false)
    private String projectScale;
}
