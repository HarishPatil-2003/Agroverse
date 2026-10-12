package com.agroverse.backend.repository;

import com.agroverse.backend.model.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    List<CommunityPost> findAllByOrderByCreatedAtDesc();
    List<CommunityPost> findByCategoryOrderByCreatedAtDesc(String category);
    List<CommunityPost> findByUserIdOrderByCreatedAtDesc(Long userId);
}
