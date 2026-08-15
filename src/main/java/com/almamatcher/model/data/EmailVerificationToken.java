package com.almamatcher.model.data;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "email_verification_token")
public class EmailVerificationToken {
    
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @NotBlank
    @Column(unique = true, nullable = false, length = 64, updatable = false)
    private String token;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    @Column
    private Instant usedAt;

    protected EmailVerificationToken() {}

    private EmailVerificationToken(
        final String token,
        final Account account,
        final Instant createdAt,
        final Instant expiresAt
    ) {
        this.token = token;
        this.account = account;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static EmailVerificationToken of(
        final Account account,
        final String token,
        final Instant createdAt,
        final Instant expiresAt
    ) {
        return new EmailVerificationToken(token, account, createdAt, expiresAt);
    }

    public void markAsUsed(final Instant now) {
        this.usedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Account getAccount() {
        return account;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public boolean isExpired(final Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isUsable(final Instant now) {
        return !isUsed() && !isExpired(now);
    }

}
