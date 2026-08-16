package com.almamatcher.model.data;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "account")
public class Account {
    
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Email
    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(nullable = false, length = 72)
    private String passwordHash;

    @Size(min = 3, max = 20)
    @Column(unique = true, nullable = false, length = 20)
    @NotBlank
    @Pattern(regexp = "^[a-z0-9_.]+$", 
                message = "solo lettere minuscole, cifre, punto e underscore")
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @NotNull
    private AccountStatus status;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant emailVerifiedAt;

    private Instant lastTimeOnlineAt;

    private Instant usernameChangedAt;

    @OneToOne(mappedBy = "account", fetch = FetchType.LAZY)
    private Profile profile;

    protected Account() {
        // for JPA
    }

    public Account(
        final String email,
        final String passwordHash,
        final String username,
        final AccountStatus status,
        final Instant createdAt,
        final Instant emailVerifiedAt,
        final Instant lastTimeOnlineAt,
        final Instant usernameChangedAt
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.username = username;
        this.status = status;
        this.createdAt = createdAt;
        this.emailVerifiedAt = emailVerifiedAt;
        this.lastTimeOnlineAt = lastTimeOnlineAt;
        this.usernameChangedAt = usernameChangedAt;
    }

    public static Account createNewAccount(
        final String email,
        final String passwordHash,
        final String username
    ) {
        return new Account(
            email,
            passwordHash,
            username,
            AccountStatus.WAIT_FOR_EMAIL_VERIFICATION,
            Instant.now(),
            null,
            null,
            null
        );
    }

    public UUID getId() {
        return this.id;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public String getUsername() {
        return this.username;
    }

    public AccountStatus getStatus() {
        return this.status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getEmailVerifiedAt() {
        return this.emailVerifiedAt;
    }

    public void setEmailVerifiedAt(final Instant newStamp) {
        this.emailVerifiedAt = newStamp;
    }

    public Instant getLastTimeOnlineAt() {
        return this.lastTimeOnlineAt;
    }

    public void setLastTimeOnlineAt(final Instant newStamp) {
        this.lastTimeOnlineAt = newStamp;
    }

    public Instant getUsernameChangedAt() {
        return this.usernameChangedAt;
    }

    public void setUsernameChangedAt(final Instant newStamp) {
        this.usernameChangedAt = newStamp;
    }

    public Profile getProfile() {
        return this.profile;
    }

    public void verifyEmail() {
        this.emailVerifiedAt = Instant.now();
        this.status = AccountStatus.ACTIVE;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Account other)) {
            return false;
        }
        return email != null && email.equals(other.email);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Account{"
            + "id=" + id
            + ", email=" + email
            + ", username=" + username
            + ", status=" + status
            + ", createdAt=" + createdAt
            + '}';
    }
}
