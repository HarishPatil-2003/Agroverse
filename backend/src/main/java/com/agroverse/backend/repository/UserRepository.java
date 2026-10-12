package com.agroverse.backend.repository;

import com.agroverse.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByFarmerId(String farmerId);
    Optional<User> findBySupabaseUserId(String supabaseUserId);
    boolean existsByFarmerId(String farmerId);
    boolean existsBySupabaseUserId(String supabaseUserId);
}
