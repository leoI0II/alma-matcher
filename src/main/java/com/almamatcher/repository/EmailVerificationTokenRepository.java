package com.almamatcher.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.almamatcher.model.data.EmailVerificationToken;

public interface EmailVerificationTokenRepository 
        extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByToken(String token);
    
}
