package com.example.trouble_log.domain.projectSession.repository;

import com.example.trouble_log.domain.projectSession.entity.PreContext;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreContextRepository extends JpaRepository<PreContext, Long> {

    boolean existsByProjectSessionId(Long sessionId);

    // 세션으로 사전 컨텍스트 조회 —> 리포트 생성 시 사용
    Optional<PreContext> findByProjectSession(ProjectSession projectSession);
}
