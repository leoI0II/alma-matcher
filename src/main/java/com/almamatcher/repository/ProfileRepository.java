package com.almamatcher.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.almamatcher.model.data.Profile;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    
    Optional<Profile> findByAccountId(UUID accountId);

    // Optional<Profile> findByAccountUsername(String username);
}
