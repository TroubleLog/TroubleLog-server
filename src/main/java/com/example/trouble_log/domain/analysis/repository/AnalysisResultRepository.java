package com.example.trouble_log.domain.analysis.repository;

import com.example.trouble_log.domain.analysis.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
}
