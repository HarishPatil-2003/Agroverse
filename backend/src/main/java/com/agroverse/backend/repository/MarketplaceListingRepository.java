package com.agroverse.backend.repository;

import com.agroverse.backend.model.MarketplaceListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, Long> {
    List<MarketplaceListing> findByStatusOrderByCreatedAtDesc(String status);
    List<MarketplaceListing> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status);
    List<MarketplaceListing> findByUserIdOrderByCreatedAtDesc(Long userId);
}
