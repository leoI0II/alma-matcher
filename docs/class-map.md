# Alma Matcher — Class Map

Scope: everything under `src/main/java/com/almamatcher` as of 2026-08-09 — 9 source classes across 6 packages, covering the registration flow only (no web/controller layer yet).

```mermaid
classDiagram
    direction LR

    class Account {
        <<Entity>>
        -UUID id
        -String email
        -String passwordHash
        -String username
        -AccountStatus status
        -Instant createdAt
        -Instant emailVerifiedAt
        -Instant lastTimeOnlineAt
        -Instant usernameChangedAt
        -Profile profile
        +createNewAccount(String email, String passwordHash, String username)$ Account
        +getId() UUID
        +getEmail() String
        +getUsername() String
        +getStatus() AccountStatus
        +setEmailVerifiedAt(Instant)
        +setLastTimeOnlineAt(Instant)
        +setUsernameChangedAt(Instant)
        +getProfile() Profile
    }

    class AccountStatus {
        <<Enumeration>>
        WAIT_FOR_EMAIL_VERIFICATION
        ACTIVE
        DEACTIVATED
    }

    class Profile {
        <<Entity>>
        -UUID id
        -String firstName
        -String lastName
        -LocalDate birthDate
        -Account account
        +getFirstName() String
        +setFirstName(String)
        +getLastName() String
        +setLastName(String)
        +getBirthDate() LocalDate
        +getAccount() Account
    }

    class RegistrationRequest {
        <<Record>>
        +String email
        +String username
        +String password
        +String firstName
        +String lastName
        +LocalDate birthDate
    }

    class UsernameAlreadyTakenException {
        <<Exception>>
    }

    class AccountRepository {
        <<Repository Interface>>
        +findByEmail(String) Optional~Account~
        +existsByUsername(String) boolean
    }

    class RegistrationService {
        <<Service>>
        -PasswordEncoder passwordEncoder
        -AccountRepository accountRepository
        +register(RegistrationRequest) void
    }

    class SecurityConfig {
        <<Configuration>>
        +passwordEncoder() PasswordEncoder
    }

    class PasswordEncoder {
        <<Spring Security>>
    }

    class JpaRepository {
        <<Spring Data>>
    }

    Account "1" -- "0..1" Profile : profile / account
    Account ..> AccountStatus : status
    AccountRepository --|> JpaRepository : extends
    AccountRepository ..> Account : Account, UUID
    RegistrationService --> AccountRepository : accountRepository
    RegistrationService --> PasswordEncoder : passwordEncoder
    RegistrationService ..> RegistrationRequest : register(request)
    RegistrationService ..> Account : createNewAccount()
    RegistrationService ..> UsernameAlreadyTakenException : throws
    SecurityConfig ..> PasswordEncoder : @Bean BCryptPasswordEncoder
```

## Legend

| Stereotype | Meaning |
|---|---|
| `<<Entity>>` | JPA-mapped class, persisted via Hibernate (`Account`, `Profile`) |
| `<<Enumeration>>` | Fixed value set (`AccountStatus`) |
| `<<Record>>` | Immutable DTO used only in-memory (`RegistrationRequest`) |
| `<<Repository Interface>>` | Spring Data interface, no implementation written by hand |
| `<<Service>>` | `@Service` business-logic component |
| `<<Configuration>>` | `@Configuration` class providing beans |
| `<<Exception>>` | Checked exception |
| `<<Spring Security>>` / `<<Spring Data>>` | Framework types outside this codebase, shown for context |

## Field notes

- **`RegistrationService.register(...)`** is likely a typo for `register` — worth renaming before a controller starts calling it.
- **No web layer yet.** `RegistrationService` currently has no caller anywhere in `src/` — registration is only reachable directly (e.g. from a test).
- **`UsernameAlreadyTakenException`** has no constructor or message, so a caller catching it learns nothing about which username collided.
- **`Profile.birthDate`** is validated with `@Past` only — there's no minimum-age check, so a birth date of yesterday currently passes validation.
- `RegistrationService.java` ends with a self-directed comment asking how `JpaRepository<T, ID>` generics resolve for `AccountRepository extends JpaRepository<Account, UUID>` — happy to walk through that if useful (short answer: `T` = the entity managed, `ID` = the type of its `@Id` field, so `Account` is the entity and `UUID` is `Account.id`'s type).
