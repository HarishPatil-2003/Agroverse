package com.agroverse.backend.repository;

import com.agroverse.backend.model.GovernmentScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovernmentSchemeRepository extends JpaRepository<GovernmentScheme, Long> {
    
    @Query("SELECT DISTINCT s FROM GovernmentScheme s WHERE " +
           "LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.category) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<GovernmentScheme> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT s FROM GovernmentScheme s WHERE s.deadlineDate >= CURRENT_DATE AND s.status = 'ACTIVE' ORDER BY s.deadlineDate ASC")
    List<GovernmentScheme> findUpcomingDeadlines();
}
