package com.agroverse.backend.repository;

import com.agroverse.backend.model.FarmTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FarmTaskRepository extends JpaRepository<FarmTask, Long> {
    List<FarmTask> findByUserIdOrderByCreatedAtAsc(Long userId);
    long countByUserIdAndCompletedFalse(Long userId);
}
