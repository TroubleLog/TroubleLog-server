package com.example.trouble_log.domain.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private Long sessionId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String requestSummary;

    @Column(columnDefinition = "TEXT")
    private String responseSummary;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuditLog(
            Long memberId,
            Long sessionId,
            String action,
            String status,
            String requestSummary,
            String responseSummary,
            String errorMessage
    ) {
        this.memberId = memberId;
        this.sessionId = sessionId;
        this.action = action;
        this.status = status;
        this.requestSummary = requestSummary;
        this.responseSummary = responseSummary;
        this.errorMessage = errorMessage;
        this.createdAt = LocalDateTime.now();
    }
}
