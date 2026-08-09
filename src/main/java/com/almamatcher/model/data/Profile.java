package com.almamatcher.model.data;

import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "profile")
public class Profile {
    
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Size(max = 30)
    @Column(nullable = false, length = 30)
    @NotBlank
    private String firstName;
    
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    @NotBlank
    private String lastName;

    @NotNull
    @Past
    @Column(nullable = false)
    private LocalDate birthDate;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", unique = true)
    private Account account;

    // list of photos in future

    protected Profile() {
        // requested by JPA
    }

    public Profile(
        final String firstName,
        final String lastName,
        final LocalDate birthDate,
        final Account account
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.account = account;
    }

    public UUID getId() {
        return this.id;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public LocalDate getBirthDate() {
        return this.birthDate;
    }

    public Account getAccount() {
        return this.account;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Profile other)) {
            return false;
        }
        return account != null && account.equals(other.account);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Profile{"
            + "id=" + id
            + ", firstName=" + firstName
            + ", lastName=" + lastName
            + ", birthDate=" + birthDate
            + '}';
    }

}
