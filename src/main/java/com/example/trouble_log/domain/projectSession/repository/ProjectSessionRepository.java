package com.example.trouble_log.domain.projectSession.repository;

import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSessionRepository extends JpaRepository<ProjectSession, Long> {

}
