package com.agroverse.backend.repository;

import com.agroverse.backend.model.SchemeApplication;
import com.agroverse.backend.model.User;
import com.agroverse.backend.model.GovernmentScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeApplicationRepository extends JpaRepository<SchemeApplication, Long> {
    List<SchemeApplication> findByUser(User user);
    Optional<SchemeApplication> findByUserAndScheme(User user, GovernmentScheme scheme);
    boolean existsByUserAndScheme(User user, GovernmentScheme scheme);
}
