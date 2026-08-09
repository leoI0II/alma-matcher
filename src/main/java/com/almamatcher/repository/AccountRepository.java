package com.almamatcher.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.almamatcher.model.data.Account;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByEmail(String email);
    
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
