# AlmaMatcher — Project Context

Context document for the assistant (Claude Code) and for myself.
**Status:** project scaffolded, domain model in progress (`Account`, `Profile`).

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

### Migrations: Flyway from the first commit

`spring.jpa.hibernate.ddl-auto=validate`, never `update`.

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

**`alma-matcher` is a root key**, at zero indentation, a sibling of `spring` and
not nested inside it. YAML nesting is determined **only** by indentation; move
it under `spring.application` and the property becomes
`spring.application.alma-matcher.*`, which Spring never sees.

Indent with 2 spaces (YAML convention; with 4, deep nesting runs off the screen).

Read it through `@ConfigurationProperties(prefix = "alma-matcher")` into a typed
class (getters and setters required), not through scattered `@Value`.

Until that class exists, the IDE underlines the properties as unknown — that's
normal and doesn't affect anything. Once it exists, the
`configuration-processor` generates metadata and autocomplete starts working.

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

---

## 14. Status

- [x] Project scaffolded (Spring Initializr, Gradle Kotlin DSL, Boot 4.1.0, Java 21)
- [x] `application.yaml` with base properties
- [x] README for GitHub
- [x] `AccountStatus`, `Account`, `Profile` — drafted
- [ ] `SecurityConfig` with `PasswordEncoder`
- [ ] `@ConfigurationProperties` class for `alma-matcher.*`
- [ ] `equals` / `hashCode` by `id` on entities
- [ ] Repositories (`AccountRepository`, `ProfileRepository`)
- [ ] PostgreSQL connection
- [ ] Flyway
- [ ] `RegistrationRequest` + `RegistrationService`
- [ ] Email verification (end-to-end delivery test)
- [ ] Controllers
- [ ] Events
- [ ] Votes and matches
- [ ] Chat
- [ ] Frontend

### Next steps

1. Finish `Account` / `Profile`, add `equals` / `hashCode` by `id`.
2. Run PostgreSQL in Docker, wire it into `application.yaml`.
3. Repositories, and a first run against a real database.
4. **End-to-end email delivery test** to a real `@studio.unibo.it` address —
   until the code arrives, building anything else is pointless.
5. LocalDate.now() in RegistrationService is not testable. Instead put Clock as bean
   and use LocalDate.now(clock) so tests could use data.
6. For PRIVACY, add the email noreply mail if the requested email in the REGISTRATION FORM 
   already exists, so the guy who's trying to register with this mail will not know this 
   sensible data.
7. Once the real registrations will be on, ENABLE CSRF protection and for cookies.
