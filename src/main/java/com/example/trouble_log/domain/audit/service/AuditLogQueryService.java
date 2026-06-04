package com.example.trouble_log.domain.audit.service;

import com.example.trouble_log.domain.audit.dto.AuditLogResponse;
import com.example.trouble_log.domain.audit.entity.AuditLog;
import com.example.trouble_log.domain.audit.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogResponse> findAuditLogs(
            String action,
            String status,
            Long memberId,
            Long sessionId,
            Pageable pageable
    ) {
        Specification<AuditLog> specification = buildSpecification(action, status, memberId, sessionId);

        return auditLogRepository.findAll(specification, pageable)
                .map(AuditLogResponse::from);
    }

    private Specification<AuditLog> buildSpecification(String action, String status, Long memberId, Long sessionId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!isBlank(action)) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }
            if (!isBlank(status)) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (memberId != null) {
                predicates.add(criteriaBuilder.equal(root.get("memberId"), memberId));
            }
            if (sessionId != null) {
                predicates.add(criteriaBuilder.equal(root.get("sessionId"), sessionId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
