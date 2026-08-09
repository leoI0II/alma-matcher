# Alma Matcher — Class Map

Scope: everything under `src/main/java/com/almamatcher` as of 2026-08-09 — 13 source classes across 6 packages, covering the registration flow only (no web/controller layer yet).

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

    class AlmaMatcherProperties {
        <<Record, ConfigurationProperties>>
        +List~String~ emailDomains
        +Username username
    }

    class Username {
        <<Record, nested>>
        +int minLength
        +int maxLength
        +String pattern
    }

    class UsernameAlreadyTakenException {
        <<Exception>>
    }

    class EmailAlreadyInUseException {
        <<Exception>>
    }

    class NotAdultEnoughException {
        <<Exception>>
    }

    class EmailDomainNotAllowedException {
        <<Exception>>
    }

    class AccountRepository {
        <<Repository Interface>>
        +findByEmail(String) Optional~Account~
        +existsByUsername(String) boolean
        +existsByEmail(String) boolean
    }

    class ProfileRepository {
        <<Repository Interface>>
        +findByAccountId(UUID) Optional~Profile~
    }

    class RegistrationService {
        <<Service>>
        -AlmaMatcherProperties properties
        -PasswordEncoder passwordEncoder
        -AccountRepository accountRepository
        -ProfileRepository profileRepository
        -isAllowedByAge(LocalDate) boolean
        -alreadyExistsByEmail(String) boolean
        -alreadyExistsByUsername(String) boolean
        -extractDomain(String) String
        -isAllowedDomain(String) boolean
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
    AlmaMatcherProperties *-- "1" Username : username
    AccountRepository --|> JpaRepository : extends
    AccountRepository ..> Account
    ProfileRepository --|> JpaRepository : extends
    ProfileRepository ..> Profile
    RegistrationService --> AccountRepository : accountRepository
    RegistrationService --> ProfileRepository : profileRepository
    RegistrationService --> PasswordEncoder : passwordEncoder
    RegistrationService --> AlmaMatcherProperties : properties
    RegistrationService ..> RegistrationRequest : register(request)
    RegistrationService ..> Account : createNewAccount()
    RegistrationService ..> Profile : new Profile()
    RegistrationService ..> UsernameAlreadyTakenException : throws
    RegistrationService ..> EmailAlreadyInUseException : throws
    RegistrationService ..> NotAdultEnoughException : throws
    RegistrationService ..> EmailDomainNotAllowedException : throws
    SecurityConfig ..> PasswordEncoder : @Bean BCryptPasswordEncoder
```

## Legend

| Stereotype | Meaning |
|---|---|
| `<<Entity>>` | JPA-mapped class, persisted via Hibernate (`Account`, `Profile`) |
| `<<Enumeration>>` | Fixed value set (`AccountStatus`) |
| `<<Record>>` | Immutable DTO used only in-memory (`RegistrationRequest`, `AlmaMatcherProperties`, `Username`) |
| `<<Repository Interface>>` | Spring Data interface, no implementation written by hand |
| `<<Service>>` | `@Service` business-logic component |
| `<<Configuration>>` | `@Configuration` class providing beans |
| `<<Exception>>` | All four now unchecked (`extends RuntimeException`) |
| `<<Spring Security>>` / `<<Spring Data>>` | Framework types outside this codebase, shown for context |

`Account` and `Profile` also override `equals`/`hashCode`/`toString` (identity on `email` and `account` respectively) — omitted from the diagram as boilerplate.

## Field notes

- The four registration exceptions are unchecked now and `register()` no longer declares `throws` — nothing in the codebase catches them yet, so once a controller exists it'll need a shared handler (e.g. `@ControllerAdvice`) to turn them into HTTP responses.
- `AlmaMatcherProperties.Username` validates `minLength`/`maxLength`/`pattern`, but nothing reads those fields — `RegistrationRequest.username` still hardcodes its own `@Size(min = 3, max = 20)` and regex independently.
- Still no web layer — `RegistrationService.register(...)` has no caller anywhere in `src/`.
