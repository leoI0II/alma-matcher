# Alma Matcher — Class Map

Scope: everything under `src/main/java/com/almamatcher` as of 2026-08-15 — 24 source classes across 9 packages, covering the registration flow end to end (controller → service → repository → entity) plus the email-verification-token issuance flow (token creation + `AccountRegisteredEvent` + listener that logs the link). No verify/confirm endpoint yet.

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

    class EmailVerificationToken {
        <<Entity>>
        -UUID id
        -String token
        -Account account
        -Instant createdAt
        -Instant expiresAt
        -Instant usedAt
        +of(Account account, String token, Instant createdAt, Instant expiresAt)$ EmailVerificationToken
        +markAsUsed(Instant now)
        +getToken() String
        +getAccount() Account
        +isExpired(Instant now) boolean
        +isUsed() boolean
        +isUsable(Instant now) boolean
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
        +EmailVerification emailVerification
    }

    class Username {
        <<Record, nested>>
        +int minLength
        +int maxLength
        +String pattern
    }

    class EmailVerification {
        <<Record, nested>>
        +Duration tokenValidity
        +String baseUrl
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

    class EmailVerificationTokenRepository {
        <<Repository Interface>>
        +findByToken(String) Optional~EmailVerificationToken~
    }

    class RegistrationService {
        <<Service>>
        -AlmaMatcherProperties properties
        -PasswordEncoder passwordEncoder
        -AccountRepository accountRepository
        -ProfileRepository profileRepository
        -TokenGenerator tokenGenerator
        -EmailVerificationTokenRepository tokenRepository
        -ApplicationEventPublisher eventPublisher
        -isAllowedByAge(LocalDate) boolean
        -alreadyExistsByEmail(String) boolean
        -alreadyExistsByUsername(String) boolean
        -extractDomain(String) String
        -isAllowedDomain(String) boolean
        +register(RegistrationRequest) void
    }

    class TokenGenerator {
        <<Component>>
        +generate() String
    }

    class EmailSenderService {
        <<Interface>>
        +sendVerification(String to, String token)
    }

    class LoggingEmailSender {
        <<Component>>
        -AlmaMatcherProperties properties
        +sendVerification(String to, String token)
    }

    class AccountRegisteredEvent {
        <<Record, Event>>
        +String email
        +String token
    }

    class VerificationEmailListener {
        <<Component>>
        -EmailSenderService emailSender
        +onAccountRegistered(AccountRegisteredEvent event)
    }

    class RegistrationController {
        <<RestController>>
        -RegistrationService registrationService
        +register(RegistrationRequest request) void
    }

    class RegistrationExceptionHandler {
        <<RestControllerAdvice>>
        +handleConflict(RuntimeException) Map
        +handleBadRequest(RuntimeException) Map
    }

    class SecurityConfig {
        <<Configuration>>
        +passwordEncoder() PasswordEncoder
        +filterChain(HttpSecurity) SecurityFilterChain
    }

    class PasswordEncoder {
        <<Spring Security>>
    }

    class JpaRepository {
        <<Spring Data>>
    }

    class ApplicationEventPublisher {
        <<Spring Context>>
    }

    Account "1" -- "0..1" Profile : profile / account
    Account ..> AccountStatus : status
    EmailVerificationToken "*" --> "1" Account : account
    AlmaMatcherProperties *-- "1" Username : username
    AlmaMatcherProperties *-- "1" EmailVerification : emailVerification
    AccountRepository --|> JpaRepository : extends
    AccountRepository ..> Account
    ProfileRepository --|> JpaRepository : extends
    ProfileRepository ..> Profile
    EmailVerificationTokenRepository --|> JpaRepository : extends
    EmailVerificationTokenRepository ..> EmailVerificationToken
    RegistrationService --> AccountRepository : accountRepository
    RegistrationService --> ProfileRepository : profileRepository
    RegistrationService --> EmailVerificationTokenRepository : tokenRepository
    RegistrationService --> PasswordEncoder : passwordEncoder
    RegistrationService --> AlmaMatcherProperties : properties
    RegistrationService --> TokenGenerator : tokenGenerator
    RegistrationService --> ApplicationEventPublisher : eventPublisher
    RegistrationService ..> RegistrationRequest : register(request)
    RegistrationService ..> Account : createNewAccount()
    RegistrationService ..> Profile : new Profile()
    RegistrationService ..> EmailVerificationToken : EmailVerificationToken.of()
    RegistrationService ..> AccountRegisteredEvent : publishEvent()
    RegistrationService ..> UsernameAlreadyTakenException : throws
    RegistrationService ..> EmailAlreadyInUseException : throws
    RegistrationService ..> NotAdultEnoughException : throws
    RegistrationService ..> EmailDomainNotAllowedException : throws
    SecurityConfig ..> PasswordEncoder : @Bean BCryptPasswordEncoder
    RegistrationController --> RegistrationService : registrationService
    RegistrationController ..> RegistrationRequest : register(request)
    RegistrationExceptionHandler ..> UsernameAlreadyTakenException : @ExceptionHandler
    RegistrationExceptionHandler ..> EmailAlreadyInUseException : @ExceptionHandler
    RegistrationExceptionHandler ..> NotAdultEnoughException : @ExceptionHandler
    RegistrationExceptionHandler ..> EmailDomainNotAllowedException : @ExceptionHandler
    VerificationEmailListener --> EmailSenderService : emailSender
    VerificationEmailListener ..> AccountRegisteredEvent : @TransactionalEventListener
    LoggingEmailSender ..|> EmailSenderService : implements
    LoggingEmailSender --> AlmaMatcherProperties : properties
```

## Legend

| Stereotype | Meaning |
|---|---|
| `<<Entity>>` | JPA-mapped class, persisted via Hibernate (`Account`, `Profile`, `EmailVerificationToken`) |
| `<<Enumeration>>` | Fixed value set (`AccountStatus`) |
| `<<Record>>` | Immutable DTO used only in-memory (`RegistrationRequest`, `AlmaMatcherProperties`, `Username`, `EmailVerification`) |
| `<<Record, Event>>` | Immutable Spring application event payload (`AccountRegisteredEvent`) |
| `<<Repository Interface>>` | Spring Data interface, no implementation written by hand |
| `<<Service>>` | `@Service` business-logic component |
| `<<Component>>` | `@Component` bean that isn't a `@Service`/`@Repository`/controller (`TokenGenerator`, `LoggingEmailSender`, `VerificationEmailListener`) |
| `<<Interface>>` | Plain Java interface, no Spring stereotype of its own (`EmailSenderService`) |
| `<<RestController>>` / `<<RestControllerAdvice>>` | Web layer: HTTP endpoints and centralised exception translation |
| `<<Configuration>>` | `@Configuration` class providing beans |
| `<<Exception>>` | All four now unchecked (`extends RuntimeException`) |
| `<<Spring Security>>` / `<<Spring Data>>` / `<<Spring Context>>` | Framework types outside this codebase, shown for context |

`Account`, `Profile` and `EmailVerificationToken` also override or rely on identity conventions — `Account`/`Profile` override `equals`/`hashCode`/`toString` (identity on `email` and `account` respectively); `EmailVerificationToken` has none of the three, omitted from the diagram as boilerplate.

## Field notes

- **Web layer now exists.** `RegistrationController` (`POST /api/auth/register`, 201) is the only caller of `RegistrationService.register(...)`. `RegistrationExceptionHandler` (`@RestControllerAdvice`) turns the four registration exceptions into 409 (duplicate email/username) or 400 (age, domain) JSON bodies — nothing surfaces as a raw 500 anymore.
- **Email verification token issuance is wired, confirmation isn't.** `register(...)` creates the `Account` and `Profile`, generates a token via `TokenGenerator` (32 random bytes, URL-safe Base64), persists an `EmailVerificationToken` with `createdAt`/`expiresAt` computed from `AlmaMatcherProperties.emailVerification().tokenValidity()`, and publishes `AccountRegisteredEvent`. `VerificationEmailListener` picks it up with `@TransactionalEventListener(phase = AFTER_COMMIT)` — the email only "sends" after the DB transaction commits, so a rolled-back registration never notifies anyone. There is still no `GET /api/auth/verify` endpoint, so `EmailVerificationTokenRepository.findByToken(...)` and `EmailVerificationToken.isUsable(...)/markAsUsed(...)` have no caller yet.
- **`EmailSenderService` has one implementation.** `LoggingEmailSender` just logs the verification link (`{baseUrl}/api/auth/verify?token=...`) via SLF4J — no real email provider is wired in yet.
- `AlmaMatcherProperties.Username` validates `minLength`/`maxLength`/`pattern`, but nothing reads those fields — `RegistrationRequest.username` still hardcodes its own `@Size(min = 3, max = 20)` and regex independently.
