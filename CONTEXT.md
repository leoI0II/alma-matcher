# AlmaMatcher — Project Context

Context document for the assistant (Claude Code) and for myself.
**Status:** registration flow works end to end — `POST /api/auth/register`
returns 201 and writes `account` + `profile` rows to PostgreSQL.

---

## 1. The idea

A closed platform for dating and shared activities, open only to University of
Bologna (UniBo) students. Sign-up is restricted to university email addresses,
which filters out bots and outsiders and creates a high-trust environment.

Where it came from: the Instagram page *Spotted Cesena UniBo* started getting a
lot of "looking for someone" posts. A comment suggesting a dedicated platform
for it got ~25 likes — first validation that the demand is real.

**Nature of the project:** a learning side project. The main goal is to learn
server-side development. If it takes off, it could be pitched to the university
for server funding.

**Distribution:** no collaboration with Spotted planned (risk of them asking for
payment or a cut). A post of my own once there's something to show is enough.

### Scope

All of UniBo, not just the Cesena campus — the email domain is shared across
campuses. A "campus" field goes into the model from the start and acts as a
filter. Launch in Cesena (that's where the community and I are), then Forlì,
Ravenna, Rimini, Bologna.

**Trade-off:** a larger user base cures the cold-start problem but dilutes the
"everyone here is one of us" feeling. Start local.

---

## 2. MVP features

1. **Profiles** — basic info, photos, faculty, campus.
2. **Events** — a board of activities (library, aperitivo, house party) people can join.
3. **Likes** — simplified yes/no instead of swiping.
4. **Matching** — a chat opens on mutual yes.

### Events matter more than dating

A campus Tinder clone dies from two problems:

- **Cold start** — with few users the feed runs out in five minutes.
- **Gender imbalance** — engineering faculties are heavily skewed; men get no
  matches and leave, women drown in likes and leave.

Events don't suffer from either: "who's going for an aperitivo on Thursday" is
useful with thirty users and is gender-neutral. Events drive retention; dating
is a layer on top.

### Ideas for later

- **Study buddy** — matching by exam ("looking for someone for Analisi B, June
  session"). Gender-neutral, useful year-round, and removes the "dating site"
  stigma that keeps people from signing up.
- **Anonymous Spotted board inside the platform** — post anonymously, the person
  mentioned can respond, identities revealed on mutual consent.
- **Weekly batch of candidates** instead of an infinite feed — with a small user
  base infinite swiping ends in an empty screen. Creates a reason to come back.
- **Match expiry** — a match with no message in 72 hours disappears. Prevents a
  graveyard of dead matches and ghosting.
- **Report / block from day one.** Small campus, everyone recognises everyone.
  One harassment incident without a working report button destroys trust — and
  trust is the project's only real asset.

---

## 3. Tech stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| ORM | Spring Data JPA (Hibernate) |
| Database | PostgreSQL |
| Build | Gradle (Kotlin DSL) |
| Packaging | Jar |
| Config | YAML |
| Local DB | Docker Compose (`postgres:17`) |
| Frontend | undecided; mobile-first PWA |
| Hosting | EU VPS (Hetzner), Docker Compose |

**Project metadata:**
`group = com.almamatcher`, `artifact = alma-matcher`, package `com.almamatcher`.
A hyphen in the group is invalid — it becomes a Java package name, and
`com.alma-matcher` does not compile.

**Layout** — from Spring Initializr (single module): `src/` and
`build.gradle.kts` at the root. Not to be confused with the `gradle init` layout
(`app/src/...`) — the two are incompatible.

### Dependencies

Generated through **start.spring.io**, not assembled by hand.

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

**Note on Security:** as soon as the starter is on the classpath, Spring
Security locks **every** endpoint behind a login form and prints a generated
password in the startup logs. That is expected (secure by default), not a
breakage. Configuration comes later, together with the real endpoints.

**Note on Spring Boot 4:** a recent major version — the overwhelming majority of
tutorials and StackOverflow answers target Boot 3. Expect examples that don't
work as written. Search with the version included.

### The `static` and `templates` folders

Created by Initializr; Spring looks in them automatically.

- **`static/`** — files served as-is: CSS, JS, images, favicon.
  `static/css/main.css` is served at `/css/main.css`.
- **`templates/`** — HTML templates rendered server-side (Thymeleaf).
  Currently inert: without `spring-boot-starter-thymeleaf` nothing renders.

Only needed for server-side rendering. With a separate SPA the backend returns
JSON and `templates` goes unused. Decision deferred.

### Docker and the local database

A **container** is an isolated process carrying its own filesystem and
libraries. Unlike a VM it shares the host kernel, so it starts in seconds
instead of a minute. An **image** (`postgres:17`) is the template; the container
is the running instance.

**Compose** is just a long `docker run` command written as YAML — and it scales
to several services later (Redis, MinIO) starting together with one command.

`compose.yaml` at the project root:

```yaml
services:
  postgres:
    image: postgres:17
    container_name: alma-matcher-db
    environment:
      POSTGRES_DB: almamatcher
      POSTGRES_USER: almamatcher
      POSTGRES_PASSWORD: devpassword
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

- **`ports: "5432:5432"`** — `host:container`. Without it the container is
  sealed and the app can't reach `localhost:5432`.
- **`volumes: pgdata:...`** — a container's filesystem is ephemeral. The named
  volume is what survives `docker compose down`.

The same file (plus the app as a second service) is what will run on the VPS —
which is why it's worth learning now: one tool covers dev and deploy.

**Kubernetes is a different thing, a layer above.** It orchestrates hundreds of
containers across many machines — scheduling, restarts, scaling, rolling
deploys. Irrelevant here: one VPS with Compose handles thousands of users. Worth
knowing it exists, not worth learning now.

---

## 4. Architectural decisions

### Auth: sessions, not JWT

Spring Security with session cookies (HttpOnly, Secure, SameSite=Lax).

Reasons:
- JWTs can't be revoked. Ban someone for harassment and their token stays valid
  until it expires. Unacceptable for a dating app.
- Sessions are the Spring Security default — they work out of the box, zero code.
- JWT makes sense with multiple independent services or native clients. This is
  a single monolith.

Tutorials push JWT because they're written for "React + separate API server".
Skip them.

**Login is by email only.** The username is never used to authenticate.

### Chat: polling, not WebSockets

MVP chat = a message table + POST to send + GET "newer than timestamp X" + the
browser asking every 3 seconds. Reliable, and writable in an evening.

WebSocket / STOMP deliberately deferred: they drag in connection lifecycle,
reconnection, separate auth over WS, and a client library. Too many unfamiliar
things at once. Upgrading later breaks almost nothing — same table, same logic,
only the delivery mechanism changes.

### Migrations: Flyway before the first real user

Target state: `ddl-auto: validate` plus versioned Flyway migrations. Never
`update` — it applies schema changes implicitly and silently.

**Right now** the setting is `create-drop`: the schema is rebuilt at every
start. While the model still changes hourly, writing a migration per edit costs
time and buys nothing. Test data is lost on restart, which doesn't matter yet.

Switch to Flyway once the entities stabilise — and definitely before any real
data exists.

### Email — critical risk, verify on day one

The entire trust model rests on a verification email actually **arriving** in an
`@studio.unibo.it` inbox. University mail servers filter aggressively. Gmail
SMTP or a bare Postfix on a VPS means spam folder, which means no project.

Needs a proper provider (Resend / Brevo / Mailgun, free tier) plus SPF/DKIM on
the domain. Test the whole path end to end before building anything else.

### File storage

Photos go to object storage (Cloudflare R2 / MinIO); the database holds only
keys. Not in the DB, not on the container's disk.

---

## 5. Application layers

```
controller  →  receives HTTP, validates the DTO
service     →  business logic: checks, hashing, decisions
repository  →  talks to the database
entity      →  the shape of the data
```

Each layer knows only the one below it. Same idea as desktop MVC.

### The Spring container and dependency injection

**There is one container** — the `ApplicationContext`. The annotations are not
containers; they're labels meaning "put this object in the container".

| Annotation | Goes on | For |
|---|---|---|
| `@Service` | my class | business logic |
| `@Repository` | my class | data access (+ translates Hibernate exceptions into Spring's) |
| `@Controller` / `@RestController` | my class | HTTP handling |
| `@Component` | my class | everything else |
| `@Configuration` + `@Bean` | factory class + method | objects from **other people's** libraries |

Technically `@Service` / `@Repository` / `@Component` do the same thing — they
differ in what they communicate to the reader.

`@Bean` is for classes I can't annotate because they aren't mine. Example,
`BCryptPasswordEncoder` from Spring Security:

```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

Dependencies are requested **through the constructor** — no `getInstance()`:

```java
@Service
public class RegistrationService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(AccountRepository accountRepository,
                               PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }
}
```

Spring reads the constructor and supplies the objects. That's **dependency
injection**; the reversal of who controls object creation is **inversion of
control**.

Difference from a classic singleton (`MessageBus.getInstance()`): the object is
**received**, not requested. What that buys:

- tests can pass fake implementations without touching the code;
- dependencies are visible in the constructor signature instead of hidden in
  method bodies;
- swapping an implementation (BCrypt → Argon2) is one line in `SecurityConfig`,
  because the declared type is the `PasswordEncoder` interface.

Technical detail: `new BCryptPasswordEncoder()` inside a `@Bean` method runs
**once**. Spring wraps `@Configuration` classes in a proxy and returns the
cached instance from the second call on. So it is a singleton — managed by the
container rather than by the class itself.

### Repositories — interfaces with no implementation

```java
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByEmail(String email);

    boolean existsByUsername(String username);
}
```

`JpaRepository<T, ID>`:
- **`T`** — which entity (`Account`)
- **`ID`** — the type of that entity's `@Id` field (`UUID`)

The second parameter is **not "key → value"** like in a `Map`. It answers "what
type does `findById` take". The inherited methods are generic:
`Optional<T> findById(ID id)` becomes `Optional<Account> findById(UUID id)`.

Free out of the box: `save`, `findById`, `findAll`, `delete`, `count`,
`existsById`.

Declared methods are parsed by grammar at startup (`findBy` / `existsBy` /
`countBy` / `deleteBy`, connectors `And` / `Or`, operators `GreaterThan`,
`Between`, `Like`, `In`, `IsNull`, `OrderBy...Desc`, limits like `findTop10By`).
Spring reflects over the entity to confirm the fields exist, then generates SQL.
**A typo in a field name means the application won't start** — not a production
failure.

The implementation is created at runtime as a **dynamic proxy** — there is no
`AccountRepositoryImpl` file. Verify with `repo.getClass().getName()`, which
prints something like `jdk.proxy2.$Proxy142`.

Java's dynamic proxies only work with interfaces, which is why repositories must
be interfaces rather than abstract classes. Side benefit: tests can supply a
`HashMap`-backed implementation with no database.

Complex queries, where the method name would become unreadable, are written by
hand with `@Query`.

### The web layer

An **endpoint** is one HTTP method + path pair the server answers. `GET /api/events`
and `POST /api/events` are two different endpoints despite the same path.

URL convention:

```
POST /api/auth/register     POST /api/auth/login
GET  /api/auth/verify       GET  /api/profiles/{username}
```

`/api/...` separates JSON endpoints from any future HTML pages and allows
applying CORS, rate limiting or logging to the whole group. `/auth/...` groups
identity-related endpoints, which makes the security rules a single line.

```java
@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(final RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody final RegistrationRequest request) {
        registrationService.register(request);
    }
}
```

- `@RestController` — a `@Controller` returning data (JSON) rather than template names
- `@RequestBody` — deserialise the JSON body into the DTO (Jackson, bundled with `starter-web`)
- **`@Valid`** — run the DTO's Bean Validation *before* entering the method.
  **Without it the annotations don't run at all.** Most common omission.
- `@ResponseStatus(HttpStatus.CREATED)` — 201 instead of 200

The controller does no logic: receive, validate, delegate.

**GET vs POST.** GET reads and is repeatable and cacheable; its parameters live
in the URL. POST creates or acts, carries a body, and is not repeatable.
Registration must be POST — a GET would put the password in the URL, and URLs
end up in browser history, server logs and the `Referer` header.

### Exception handling

The four registration exceptions are unchecked and nobody catches them, so
without a handler they'd all surface as 500. One central class translates them:

```java
@RestControllerAdvice
public class RegistrationExceptionHandler {

    @ExceptionHandler({UsernameAlreadyTakenException.class, EmailAlreadyInUseException.class})
    @ResponseStatus(HttpStatus.CONFLICT)          // 409
    public Map<String, Object> handleConflict(final RuntimeException ex) { ... }

    @ExceptionHandler({NotAdultEnoughException.class, EmailDomainNotAllowedException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)       // 400
    public Map<String, Object> handleBadRequest(final RuntimeException ex) { ... }
}
```

**Unchecked exceptions are the Spring convention** for two reasons: a
`@RestControllerAdvice` handles them centrally so no `try/catch` is scattered
around, and — critically — **`@Transactional` only rolls back automatically on
unchecked exceptions.** On checked ones it commits by default.

**Import trap:** use `org.springframework.transaction.annotation.Transactional`,
not `jakarta.transaction.Transactional`. Both compile; Spring's has `readOnly`,
`propagation`, `isolation` and integrates properly.

### Security configuration

```java
@Bean
public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**", "/error").permitAll()
            .anyRequest().authenticated()
        )
        .build();
}
```

Spring Security is a **chain of filters** in front of the controllers; every
request passes through them. This bean describes that chain — without it, the
default configuration locks everything behind a login form.

- `.permitAll()` on `/api/auth/**` — you can't require a login in order to register.
- **`/error` must be whitelisted too.** On a failed request Spring forwards
  internally to `/error` to build the response; if that path is protected the
  forward is blocked and the client gets an empty **403 instead of a 400** with
  the actual message. Confusing symptom, trivial cause.
- `.anyRequest().authenticated()` — a **whitelist**: list what's open, everything
  else is closed. Every new endpoint is protected by default. A blacklist would
  leave new endpoints exposed until remembered.

**Response headers** Spring Security adds automatically: `X-Frame-Options: DENY`
(no embedding in an iframe — anti-clickjacking), `X-Content-Type-Options:
nosniff` (don't guess file types), `Cache-Control: no-store` (never cache
personal data), `X-XSS-Protection: 0` (disables an old browser filter that
caused more harm than good).

`csrf.disable()` is temporary — see the deferred list.

---

## 6. Data model conventions

### Identifiers: UUID everywhere

```java
@Id
@GeneratedValue
@UuidGenerator(style = UuidGenerator.Style.TIME)
private UUID id;
```

A sequential `Long` in a URL leaks information: the user count becomes visible
and profiles can be enumerated. Unacceptable for a dating app.

`TIME` style produces time-ordered UUIDs — still unguessable, but without the
index fragmentation that random UUIDv4 causes.

Uniform across all entities. Never mixed with `Long`.

**Traps:**
- `GenerationType.IDENTITY` **does not work** with UUID — it's auto-increment,
  integers only. Use Hibernate's `@UuidGenerator`.
- Import `java.util.UUID`, **not** `org.hibernate.validator.constraints.UUID`
  (that one is a validation annotation, and autocomplete offers it first).
- Never put `@Column(unique = true)` on `@Id` — a `PRIMARY KEY` is already
  unique and NOT NULL; the annotation would create a second redundant index.

### Naming

- Java: `Account`, `Profile`, `EventParticipant` (PascalCase).
- DB: `account`, `profile`, `event_participant` (snake_case, Hibernate converts).
- **Never `user`** — reserved word in PostgreSQL. Hence `Account` rather than
  `User` / `AppUser`.
- A foreign key column is named after the entity it **points to**: `account_id`
  inside the `profile` table. Not `profile_id` — that says nothing about where
  the reference leads.

### Account / Profile split

**The criterion is not "private vs public" but "permanent identity vs mutable
content".**

| `Account` | `Profile` |
|---|---|
| who you are in the system | how you present yourself |
| email (private), username (public), password hash, status | first name, last name, birth date, photos, course, campus |
| survives deactivation | wiped on deactivation |

Practical benefit: rendering someone else's profile loads only `Profile` — the
email and password hash never enter memory at all. No need to remember to
exclude them; they aren't there. With thirty event participants that matters.

The email is **never** shown on a profile page (same as Instagram, Tinder, and
everywhere else). It's contact and account data, not profile data.

### Username

- **Public**, lives in `Account`.
- **Reserved** — it must outlive the profile. Otherwise, after deactivation
  `mario_r` frees up, someone else takes it, and old `@mario_r` mentions in chat
  start pointing at a different person. In an app where people meet in real
  life, that's a safety issue, not a cosmetic one.
- Uniqueness must be enforced by the **database** (`unique = true`), not by a
  service-level check: two simultaneous sign-ups are a real scenario.
- **Immutable for now.** Opening it up later is easy; closing it after people
  rely on it is not.
- Pattern `^[a-z0-9_.]+$`, length 3–20.

**Decision to make later (when building chat):** store **ids, not usernames**,
in message text — `"ciao @[9f2b-...-4a1c]"` — and resolve to the current
username at render time. Mentions then always point at the right person,
renaming breaks nothing, and old usernames can safely be released. Same
mechanism as Discord/Slack. **Decide this while writing the chat** — otherwise
the whole message history needs reprocessing.

If the username ever becomes changeable, rate-limit it (once every 30–60 days)
via the `usernameChangedAt` field. Otherwise renaming becomes a vector for
"change name, disappear, come back as someone else".

### Passwords

- Only the **BCrypt hash** is stored — 60 characters, column `length = 72`.
- **No `unique` on the hash.** First, the salt means it would never trigger.
  Second, if it did trigger it would leak: a user would learn that someone else
  has the same password.
- Password rules (length, composition) belong in the **registration DTO**, where
  the password is still plaintext. On the entity they're meaningless — the field
  holds a hash, and `@Size(max = 20)` or `@Pattern` would reject it.
- No restrictive regex on passwords. Forbidding characters makes passwords
  **worse**. A decent minimum length (12+) is enough.

A hash is a **one-way** function; no inverse algorithm exists. On login the
password is hashed again and the hashes are compared — the original is never
recovered. Hence "reset password", never "here's your password".

The salt is a random per-user string mixed in before hashing. BCrypt embeds it
in the output automatically:

```
$2a$10$N9qo8uLOickgx2ZMRZoMye IjZAgcfl7p92ldGxad68LJZdL17lhWy
 │  │  └──── salt (22) ─────┘ └──────────── hash ────────────┘
 │  └── cost (2^10 iterations)
 └── algorithm
```

SHA-256 / MD5 are **not suitable** — they're fast, and a GPU tries billions per
second. BCrypt / Argon2 are deliberately slow (~100 ms per hash).

Never implement hashing by hand: `passwordEncoder.encode(raw)` and
`passwordEncoder.matches(raw, hash)`.

### Email

Normalise to lowercase before saving. `Mario@studio.unibo.it` and
`mario@studio.unibo.it` are the same mailbox, but `unique` sees two different
values — so the same person could register twice.

### Match is materialised, not derived

`Vote` is the raw fact ("who, about whom, yes/no"), unique on
`(voter_id, target_id)` for idempotency.

`Match` is its own entity, created when a reciprocal YES is found. Reason: a
match carries the conversation, an "unmatched" state (votes stay, so the person
doesn't resurface in the feed), a creation time, a last-message time, and
expiry. None of that has anywhere to live without a match row.

**Canonical ordering:** always `account_a_id < account_b_id`, plus
`UNIQUE(account_a_id, account_b_id)`. Otherwise two simultaneous likes from two
devices create two rows for the same match.

### Collections in entities

Keep a `@OneToMany` **only when the list is small and needed in full**.

- `Event.participants` (~30 people, all shown at once) — fine.
- `Account.votes`, `Account.matches` (thousands of rows, never needed whole) —
  no. Use a targeted query instead:
  `voteRepository.existsByVoterIdAndTargetId(...)`.

`List<UUID>` instead of `List<Entity>` doesn't help — the list still loads
entirely.

### Message attaches directly to Match

No intermediate `Conversation` entity. It appears when event group chat does.

### Blocks are enforced in the feed's SQL

Not filtered afterwards in the service — otherwise a blocked user surfaces in
the results.

### Account deletion

`status = DEACTIVATED` plus wiping personal data — **not** `DELETE CASCADE`.
Otherwise deleting an account erases half of someone else's conversation, and
frees up the username.

---

## 7. Current domain model

### `AccountStatus`

```java
public enum AccountStatus {
    WAIT_FOR_EMAIL_VERIFICATION,
    ACTIVE,
    DEACTIVATED
}
```

### `Account`

Fields: `id` (UUID), `email` (unique, lowercase), `passwordHash` (length 72),
`username` (unique, immutable), `status`, `createdAt`, `emailVerifiedAt`,
`lastTimeOnlineAt`, `usernameChangedAt`, `profile` (inverse side).

Key points:

```java
@NotNull
@Column(nullable = false, updatable = false)
private Instant createdAt;

@Column                     // nullable! null = not yet verified
private Instant emailVerifiedAt;

@OneToOne(mappedBy = "account", fetch = FetchType.LAZY)
private Profile profile;
```

**Activation via a method, not setters:**

```java
public void verifyEmail() {
    this.emailVerifiedAt = Instant.now();
    this.status = AccountStatus.ACTIVE;
}
```

Two separate setters would allow the status and the timestamp to drift apart.
A method expresses the operation and makes the inconsistent state unreachable.

**`createdAt` and `emailVerifiedAt` are different things; both are needed.**
`createdAt` is what lets me delete unverified accounts older than 24 hours —
and those have `emailVerifiedAt == null`.

`@CreationTimestamp` would also work (it fires once on INSERT, not on every
load), but `Instant.now()` in the factory makes the value available immediately,
before saving. Kept the latter. `updatable = false` protects it at the DB level.

### `Profile`

Fields: `id` (UUID), `firstName`, `lastName`, `birthDate` (`@Past`), `account`
(owning side). Photos, course, campus and bio come later.

```java
@OneToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "account_id", unique = true)
private Account account;
```

### Direction of the relationship — important

The `account_id` column physically lives in the `profile` table. Therefore:

- **`Profile` knows `Account`** — `@JoinColumn`, owning side, `Account` is
  passed to the constructor.
- **`Account` does not know `Profile` at creation** — `mappedBy = "account"`,
  the field is populated by Hibernate when loading from the DB.

`@JoinColumn` = "the column is here, and this is its name" (owning side).
`mappedBy` = "the column isn't here, it's in the other class, in the field I'm
naming" (inverse side). The value of `mappedBy` is the **Java field name**, not
a column name.

**Easy mistake:** passing `Profile` into `Account`'s constructor while also
passing `Account` into `Profile`'s — chicken and egg, neither object can be
built. The correct order:

```java
Account account = Account.createNewAccount(email, hash, username);
accountRepository.save(account);

Profile profile = new Profile(firstName, lastName, birthDate, account);
profileRepository.save(profile);
```

`unique = true` on the `@JoinColumn` is what makes the relationship genuinely
one-to-one; without it the database allows two profiles per account.

### DTOs

A **Data Transfer Object** exists only to carry data between layers. Not an
entity, never persisted, no logic.

The registration form matches no entity: it carries the password **in
plaintext** (which `Account` doesn't have) and mixes fields from `Account` and
`Profile`. So it gets its own `record`:

```java
public record RegistrationRequest(
    @Email @NotBlank
    String email,

    @NotBlank @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-z0-9_.]+$")
    String username,

    @NotBlank @Size(min = 12, max = 100)
    String password,              // ← plaintext, only here

    @NotBlank @Size(max = 30)
    String firstName,

    @NotBlank @Size(max = 30)
    String lastName,

    @NotNull @Past
    LocalDate birthDate
) {}
```

Flow:

```
browser → RegistrationRequest → validation → service
                                                ↓
                                   encoder.encode(password)
                                                ↓
                                      Account + Profile → DB
```

### `RegistrationService`

Order of checks in `register(...)`, all inside `@Transactional`:

1. **Normalise the email once, at the top** — `request.email().trim().toLowerCase()`.
   Every later check must use that variable. Checking the raw value and saving
   the normalised one means `Mario@...` passes the duplicate check and then
   explodes on the DB constraint at `save()`.
2. Email domain in `properties.emailDomains()`
3. Email not already registered
4. Username not taken
5. Age ≥ 18
6. Hash the password, create `Account`, save, create `Profile`, save

**`@Transactional` is mandatory here.** Two separate `save()` calls without it
are two transactions: if the second fails, an `Account` exists with no
`Profile` — a registered user who exists nowhere. Rule: any service method
writing more than one row is `@Transactional`.

**Extracting the domain:**

```java
final int at = email.lastIndexOf('@');
return email.substring(at + 1);
```

`lastIndexOf`, not `indexOf` — a quoted local part may legally contain `@`
(`"weird@name"@studio.unibo.it`). Never seen in practice, but it costs nothing
to be right. And compare the extracted domain exactly against the list, not
`endsWith("unibo.it")` — that would accept `mario@fake-unibo.it`.

Consequence: subdomains aren't covered. If UniBo turns out to use others, they
go in the YAML list — which is exactly why the list lives in config.

### Custom validation: `@MinimumAge`

`@Past` only checks the date is in the past — someone born yesterday passes.
Age checks aren't a standard Bean Validation constraint, so it's a custom one:

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinimumAgeValidator.class)
public @interface MinimumAge {
    int value();
    String message() default "You must be at least {value} years old";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

```java
public class MinimumAgeValidator implements ConstraintValidator<MinimumAge, LocalDate> {
    private int minimumAge;

    @Override public void initialize(MinimumAge annotation) {
        this.minimumAge = annotation.value();
    }

    @Override public boolean isValid(LocalDate birthDate, ConstraintValidatorContext ctx) {
        if (birthDate == null) return true;      // @NotNull's job
        return Period.between(birthDate, LocalDate.now()).getYears() >= minimumAge;
    }
}
```

Why in the DTO rather than the service: the error comes back **together with
all other validation errors**, in one uniform response. With a service
exception the user fixes the format errors, retries, and only then learns
they're too young — an extra round trip.

**Bean Validation is server-side and is the only trustworthy check** — anyone
can bypass the browser with curl. Client-side validation (HTML5 `max`,
JavaScript) is for instant feedback only. Always both, never only the client.

In the other direction, a `ProfileResponse` carries name, photos and username —
the email simply isn't in it. Same protection the `Account`/`Profile` split
gives, applied at the network boundary.

---

## 8. JPA rules

| Rule | Why |
|---|---|
| `@Enumerated(EnumType.STRING)` **always** | The default `ORDINAL` stores the position. Insert a constant in the middle of the enum and every stored value silently means something else. |
| `fetch = FetchType.LAZY` on every `@ManyToOne` and `@OneToOne` | The default is `EAGER`. Load 100 rows → 101 queries (the N+1 problem). |
| `mappedBy` on the inverse side | Without it Hibernate creates an extra column or a join table. |
| Never `@Data` (Lombok) on entities | Generated `equals`/`hashCode` cover every field, hit the DB for collections, and break once the id is assigned. Fine on DTOs. |
| `Instant` for points in time, `LocalDate` for birth dates | Not `Date`, not `LocalDateTime` where the timezone matters. |
| Never return entities from a controller | DTOs / records only. Lazily loaded fields blow up during serialisation. |
| `protected` no-arg constructor | JPA instantiates by reflection. `protected` stops my own code creating an empty object by accident. |

**There are no "Jakarta data types".** Jakarta provides annotations and APIs;
the types come from Java: `String`, `Integer`, `LocalDate`, `Instant`, `UUID`.
`jakarta.persistence.criteria.*` is the query-building API, not data types.

### Annotation reference

**Class and table**
- `@Entity` — maps the class to a table (mandatory)
- `@Table(name = "...")` — explicit table name
- `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {...}))` — composite uniqueness

**Identifier**
- `@Id`, `@GeneratedValue`, `@UuidGenerator(style = Style.TIME)`

**Fields**
- `@Column(name, nullable, unique, length, updatable)` — only when deviating from the default
- `@Enumerated(EnumType.STRING)`
- `@Transient` — don't persist (computed fields)
- `@Lob` — long text (`TEXT` instead of `VARCHAR`)

**Relationships**
- `@ManyToOne(fetch = LAZY, optional = false)` + `@JoinColumn(name = "...")` — the main tool
- `@OneToMany(mappedBy = "...")` — inverse side
- `@OneToOne` — one to one

**Timestamps**
- `@CreationTimestamp`, `@UpdateTimestamp` (Hibernate, not standard JPA)

**`@Column` vs `@JoinColumn`:** the first is data, the second is a foreign key
(another table's id). `@JoinColumn` always sits next to `@ManyToOne` /
`@OneToOne`, never alone.

### Validation

Two independent levels, both needed:

- **Bean Validation** (`@NotBlank`, `@Size`, `@Email`, `@Past`, `@Pattern`) —
  checked in Java, produces readable messages for the user.
- **Database constraints** (`nullable`, `length`, `unique`) — the guarantee that
  bad data never lands, even with a bug in the logic.

**`@NotBlank` only works on strings** (non-null, non-empty, not whitespace).
On `Instant`, enums or numbers it throws `UnexpectedTypeException` at runtime.
Use `@NotNull` for those.

**`@Size` doesn't reject null** — a null value passes. A required field needs
`@NotBlank` or `@NotNull` as well.

Value objects (`@Embeddable`) later, if manual validation gets tedious. For now
`String` plus annotations.

---

## 9. Configuration

`src/main/resources/application.yaml` (Initializr created `.yaml`; `.yml` and
`.yaml` are equivalent).

```yaml
spring:
  application:
    name: alma-matcher

  datasource:
    url: jdbc:postgresql://localhost:5432/almamatcher
    username: almamatcher
    password: devpassword

  jpa:
    hibernate:
      ddl-auto: create-drop      # temporary — see migrations
    properties:
      hibernate:
        format_sql: true
    show-sql: true

# alma-matcher props
alma-matcher:
  email-domains:
    - studio.unibo.it
    - unibo.it
  username:
    min-length: 3
    max-length: 20
    pattern: "^[a-z0-9_.]+$"
```

`show-sql: true` prints every query Hibernate generates — very useful while
learning, noisy later. The `?` placeholders are **prepared statement
parameters**: values travel separately from the SQL text, which is what makes
SQL injection impossible.

Plaintext credentials are fine while this is local and the values are
dev-only. On deploy they become environment variables
(`SPRING_DATASOURCE_PASSWORD` etc.).

**`alma-matcher` is a root key**, at zero indentation, a sibling of `spring` and
not nested inside it. YAML nesting is determined **only** by indentation; move
it under `spring.application` and the property becomes
`spring.application.alma-matcher.*`, which Spring never sees.

Indent with 2 spaces (YAML convention; with 4, deep nesting runs off the screen).

Read through `@ConfigurationProperties(prefix = "alma-matcher")` into a typed
`record`, not scattered `@Value`. With a record Spring uses **constructor
binding**, so no getters or setters are needed (a normal class would need them).

```java
@Validated
@ConfigurationProperties(prefix = "alma-matcher")
public record AlmaMatcherProperties(
    @NotEmpty List<String> emailDomains,
    @NotNull @Valid Username username
) {
    public record Username(
        @Min(3) int minLength,
        @Min(3) int maxLength,
        @NotBlank String pattern
    ) {}
}
```

**Relaxed binding** maps `email-domains` → `emailDomains` automatically.

**The class does nothing until registered.** Add `@ConfigurationPropertiesScan`
on the main application class — once, and every future properties record works
automatically. (The alternative, `@EnableConfigurationProperties(X.class)`,
must list each class by hand.)

`@Validated` makes the app **fail at startup** with a clear message if the YAML
is missing a key. Without it, it starts with a null list and crashes on the
first registration — in production.

Note: `AlmaMatcherProperties.Username` is validated but **nothing reads it yet** —
`RegistrationRequest` still hardcodes its own `@Size` and regex independently.
Either wire it up or drop it.

`@Configuration` and `@ConfigurationProperties` are unrelated despite the names:
the first declares a class holding `@Bean` factory methods, the second fills a
class's fields from the YAML.

Until the properties class exists the IDE underlines the keys as unknown —
harmless. Once it does, `configuration-processor` generates metadata and
autocomplete starts working.

**Domains as a list:** UniBo master's and PhD students use `@unibo.it`; locally
it's convenient to add my own address for testing.

**Always quote regexes** — otherwise YAML may swallow `^`, `*`, `[`.

**Do not write a regex for first and last names.** Real surnames include
`D'Angelo`, `De Luca`, `Müller`, `Nguyễn`, `Rossi-Bianchi` — apostrophes,
spaces, hyphens, diacritics, non-Latin scripts. Any regex will reject someone's
real name. Check length and control characters only. For the **username** a
regex is appropriate — it goes in URLs and the user invents it.

No password regex in the config — length only, in the DTO.

---

## 10. Legal

### GDPR

The European data protection regulation. It applies to anyone processing
Europeans' data. Users being adults doesn't exempt anything — the regulation is
about transparency and control, not age.

Minimum before the first real user (no rush while it's just code):

- Privacy Policy: what's collected, why, where it's stored, how to delete it
- A working "delete my account" button
- EU hosting
- **Separate explicit consent** for partner-preference data — formally a special
  category under Art. 9, alongside health and religion. It can't be bundled into
  a general consent checkbox.

*I'm not a lawyer. Read this properly if the project grows.*

### The name

"Alma Mater Studiorum" is UniBo's official legal name and a registered brand.
The risk isn't the word "alma" — it's that "AlmaMatcher" plus
`@studio.unibo.it` addresses together suggest an official university project.

No UniBo branding is used. Renaming would be painless. The README states the
project is not affiliated with the university.

---

## 11. Glossary

| Term | Meaning |
|---|---|
| **SQL** | The query language for databases. A language, not a program. |
| **PostgreSQL** | A specific DBMS that speaks SQL. From *post-Ingres*. Written `PostgreSQL` or `Postgres`, never "Postgre". |
| **JPA** | Jakarta Persistence API — a specification (a document) describing annotations and methods. It executes nothing itself. |
| **Hibernate** | The JPA implementation. Generates SQL from annotated classes. |
| **ORM** | Object-Relational Mapping. The bridge between Java objects and flat tables. |
| **Spring Data JPA** | A layer above JPA. Parses repository method names by grammar and generates SQL at startup. A typo fails the startup. |
| **ApplicationContext** | Spring's container: holds every created object (bean). |
| **Bean** | An object managed by Spring. |
| **DI / IoC** | Dependency Injection — dependencies arrive through the constructor. Inversion of Control — the framework decides when objects are created, not my code. |
| **Dynamic proxy** | An object built at runtime to satisfy an interface, forwarding calls to a handler. This is how repositories work. |
| **DTO** | Data Transfer Object — carries data between layers. Not an entity. |
| **Hash** | A one-way function. No inverse operation exists. |
| **Salt** | A random per-user string mixed in before hashing. Defeats rainbow tables and hides matching passwords. |
| **BCrypt** | A deliberately slow password hashing algorithm with a built-in salt. |
| **JWT** | A token carrying signed content inside. The server keeps no state, but also can't revoke it. |
| **Session** | The server issues a random id in a cookie and remembers the mapping. Revocable instantly. |
| **SSE** | Server-Sent Events. An open connection the server pushes data over. One-directional. |
| **WebSocket** | The same, but bidirectional and binary-capable. |
| **STOMP** | A protocol on top of WebSocket: topics and subscriptions. |
| **Polling** | The browser asks the server every N seconds. Simple and reliable. |
| **PII** | Personally Identifiable Information. |
| **N+1** | One query for a list plus one query per element. The main cause of slow JPA code. |
| **Starter** | An empty Spring Boot package that pulls in a coherent set of libraries. |
| **Endpoint** | One HTTP method + path pair the server answers. |
| **Tomcat** | The HTTP server: opens port 8080, parses requests, hands them to Spring. Boot embeds it in the process, which is why `main()` never returns. Jetty and Undertow are drop-in alternatives. |
| **curl** | Command-line HTTP client. `-X` method, `-H` header, `-d` body, `-i` show status line, `-v` verbose. Downloading a file with curl is just a GET. |
| **URI vs URL** | Every URL is a URI. A URL also says *how and where* to reach the resource (`https://...`); a bare URI may only identify it (`urn:isbn:...`). Interchangeable in practice on the web. |
| **CSRF** | Cross-Site Request Forgery: a malicious page triggers a request to your site, and the browser attaches the session cookie automatically. Defended with a per-session token the attacker can't read. |
| **Whitelist / blacklist** | Whitelist = list what's allowed, deny the rest (secure default). Blacklist = the opposite (one forgotten entry is a hole). |
| **Container** | An isolated process with its own filesystem, sharing the host kernel. Lighter and faster than a VM. |
| **Image** | The template a container is created from (`postgres:17`). |
| **Volume** | Docker-managed disk space that outlives the container. Without it, database data disappears on `down`. |
| **Kubernetes** | Orchestrates many containers across many machines. A layer above Docker; not needed here. |

---

## 12. The key mental shift

My background is desktop Java/JavaFX. There the object graph lives in memory for
as long as the program runs, and it is the source of truth.

On a server it's different:

- The app runs for months; memory is finite
- Every deploy restarts it and everything in memory is gone
- Dozens of concurrent threads; a shared mutable graph means race conditions

**The database is the source of truth. Memory is a scratch buffer for one
request (~50 ms).**

```
Browser → Controller → Service → Repository → PostgreSQL
          └────── objects live ~50 ms ─────┘   data lives for years
```

Hence all the rules about collections: there is no long-lived `Account` object
for a `List<Vote>` to accumulate in. Every request builds a fresh object from
the database, and it dies milliseconds later.

None of this means less Java — classes, interfaces and business logic are all
still there. One thing changes: objects outlive a restart, so they have ids and
they're flat rather than a tree of references.

The database schema and the Java classes are **the same piece of work**, not two
separate projects: annotated JPA classes *are* the table definitions.

---

## 13. Git and environment

- Repository branch: `main`.
- **Must be committed:** `gradlew`, `gradlew.bat`, `gradle/wrapper/` — the
  Gradle Wrapper, which is what lets the project build without Gradle installed.
- **Must not be committed:** `build/`, `.gradle/` — already in Initializr's
  `.gitignore`.
- If `gradlew` won't run: `chmod +x gradlew`.
- Shell is **fish**, not bash. Patterns like `.[!.]*` that match nothing raise an
  error and abort the whole command.
- Editor is VS Code. After changing dependencies: *Java: Clean Java Language
  Server Workspace*, or `./gradlew build`.

### Running it

```fish
docker compose up -d          # start the database in the background
./gradlew bootRun             # start the app — it does NOT exit, Ctrl+C to stop
```

`bootRun` staying alive is correct: a web server listens until stopped. Use a
second terminal to test.

On Arch/CachyOS the Compose plugin is a separate package: `sudo pacman -S
docker-compose`. The daemon needs `sudo systemctl enable --now docker`, and
`sudo usermod -aG docker $USER` (then log out and back in) avoids `sudo` every
time — note that group is effectively root access.

Other useful commands: `docker compose ps`, `docker compose logs -f postgres`,
`docker compose down` (keeps data), `docker compose down -v` (wipes the volume).

### Inspecting the database

```fish
docker exec -it alma-matcher-db psql -U almamatcher -d almamatcher
```

```sql
\dt                 -- list tables
\d account          -- structure: types, constraints, indexes
SELECT id, email, username, status, created_at FROM account;
\q
```

Worth looking at once: `status` stored as `WAIT_FOR_EMAIL_VERIFICATION` in full
(proof `@Enumerated(EnumType.STRING)` works — the default would store an
unreadable `0`), and `password_hash` as a 60-char BCrypt string with no trace of
the original password.

### Testing registration

```fish
curl -i -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"mario.rossi@studio.unibo.it","username":"mario_r",
       "password":"unapasswordlunga","firstName":"Mario","lastName":"Rossi",
       "birthDate":"2003-04-15"}'
```

Cases to check: valid → **201**; repeat → **409**; `@gmail.com` → **400**;
`birthDate: "2015-01-01"` → **400**; short password → **400**.

### Reading Spring stack traces

They run to hundreds of lines. Either scroll from the top to the first readable
`WARN`/`ERROR` line, or read from the bottom for the first `Caused by`. The
`at org.hibernate...` frames are internal and almost never useful.

Example: `Connection to localhost:5432 refused` on line 30 was the real cause;
the 130 lines of "Unable to determine Dialect" below it were consequences.

---

## 14. Learning resources

1. **[MDN's HTTP guide](https://developer.mozilla.org/en-US/docs/Web/HTTP)** —
   free, and the foundation for everything else. Methods, status codes, headers,
   cookies, CORS. Without it Spring looks like arbitrary magic. Also the best
   preparation for next semester's Web course.
2. **"Spring Start Here" — Laurentiu Spilca.** Starts from the container and
   beans; doesn't assume dependency injection is already familiar.
3. **"Spring Security in Action" — same author.** Sessions, authentication,
   CSRF, filters. Read it when the real login arrives.
4. **"High-Performance Java Persistence" — Vlad Mihalcea.** JPA/Hibernate in
   depth; more advanced, worth a few months from now. His blog is the best free
   Hibernate resource anywhere.

Books target Boot 3 while this project is on 4 — concepts identical, occasional
API differences.

Note: **Next.js is frontend**, not an alternative to Spring. In a typical modern
stack they coexist — a JS framework in the browser, a backend serving the API.

---

## 15. Status

- [x] Project scaffolded (Spring Initializr, Gradle Kotlin DSL, Boot 4.1.0, Java 21)
- [x] `application.yaml` with base properties
- [x] README for GitHub
- [x] `AccountStatus`, `Account`, `Profile` entities (with `equals`/`hashCode`)
- [x] `SecurityConfig` with `PasswordEncoder` and `SecurityFilterChain`
- [x] `AlmaMatcherProperties` via `@ConfigurationProperties`
- [x] `AccountRepository`, `ProfileRepository`
- [x] PostgreSQL via Docker Compose, connected
- [x] `RegistrationRequest` DTO with `@MinimumAge(18)`
- [x] `RegistrationService` — domain check, duplicates, age, hashing, `@Transactional`
- [x] Four registration exceptions (unchecked) + `@RestControllerAdvice`
- [x] `RegistrationController` — **`POST /api/auth/register` returns 201** ✅
- [ ] Email verification (token, sending, confirm endpoint)
- [ ] Login / logout
- [ ] Flyway (once entities stabilise)
- [ ] Profile endpoints
- [ ] Events
- [ ] Votes and matches
- [ ] Chat
- [ ] Frontend

### Deferred — decided but not done

Each of these was a conscious decision to postpone, not an oversight.

1. **Injectable `Clock`.** `LocalDate.now()` in the age check reads system time,
   so "what happens on the day someone turns 18" can't be tested. Register a
   `Clock` bean and use `LocalDate.now(clock)`.
2. **User enumeration on registration.** Replying "this email is already
   registered" lets anyone check whether a specific person uses AlmaMatcher —
   and institutional addresses are guessable (`name.surname@studio.unibo.it`).
   On a dating app that's sensitive. Standard fix: always reply "we've sent you
   an email", and if the address already exists send *that mailbox* a "someone
   tried to register with your address" notice. The real owner understands;
   a prober learns nothing. Username collisions can stay explicit — usernames
   are public by design.
3. **Re-enable CSRF** before any real session-based login exists. Currently
   disabled only so curl can POST without fetching a token first.
4. **Flyway + `ddl-auto: validate`** before any real data exists.
5. **`AlmaMatcherProperties.Username` is unused** — `RegistrationRequest`
   duplicates the constraints. Wire it or delete it.

### Next step

**Email verification, end to end.** The riskiest part of the whole project:
until a code actually lands in a real `@studio.unibo.it` inbox, everything else
is built on an assumption. Test delivery before building the token flow.
