package com.agroverse.backend.repository;

import com.agroverse.backend.model.CropAnalysisHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CropAnalysisRepository extends JpaRepository<CropAnalysisHistory, Long> {
    List<CropAnalysisHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CropAnalysisHistory> findByFarmerIdOrderByCreatedAtDesc(String farmerId);
}
