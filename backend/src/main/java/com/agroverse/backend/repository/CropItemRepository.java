package com.agroverse.backend.repository;

import com.agroverse.backend.model.CropItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CropItemRepository extends JpaRepository<CropItem, Long> {
    List<CropItem> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CropItem> findByUserIdAndArchivedFalseOrderByCreatedAtDesc(Long userId);
    List<CropItem> findByUserIdAndArchivedTrueOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndArchivedFalse(Long userId);
}
