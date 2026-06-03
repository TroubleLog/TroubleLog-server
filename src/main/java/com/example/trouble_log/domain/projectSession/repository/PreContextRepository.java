package com.example.trouble_log.domain.projectSession.repository;

import com.example.trouble_log.domain.projectSession.entity.PreContext;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreContextRepository extends JpaRepository<PreContext, Long> {

    boolean existsByProjectSessionId(Long sessionId);
}
