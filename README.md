# AlmaMatcher

A closed social platform for University of Bologna students — meet people and
organise events, with registration restricted to verified university email
addresses.

> **Status: in development.** Registration and email verification work end to
> end. There is no login, no frontend, and no dating or events functionality
> yet.

---

## Idea

UniBo students increasingly use public Instagram pages to look for dates and
company. Those pages were never built for it: no verification, no structure, no
way to follow up.

AlmaMatcher restricts sign-up to `@studio.unibo.it` and `@unibo.it` addresses.
That single constraint filters out bots and outsiders and creates a space where
everyone is a verified student.

Two things live side by side:

- **Events** — a board where anyone can propose an activity (a study session, an
  aperitivo, a trip) and others can join. Useful from day one, regardless of how
  many people are on the platform.
- **Matching** — a simple yes/no mechanic. A conversation opens when the
  interest is mutual.

Events come first by design. A dating feed needs a critical mass of users before
it works at all; an events board is useful with thirty.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4 |
| Persistence | Spring Data JPA (Hibernate) |
| Database | PostgreSQL 17 (Docker Compose) |
| Security | Spring Security (BCrypt, session-based) |
| Build | Gradle (Kotlin DSL) |
| Frontend | not decided — mobile-first PWA planned |

### Some deliberate decisions

**Session-based auth, not JWT.** This is a single monolith with a browser
client. Sessions are the Spring Security default and, more importantly, can be
revoked instantly — a banned account loses access immediately rather than
whenever its token happens to expire.

**`Account` and `Profile` are separate entities.** `Account` holds permanent
identity (email, username, password hash, status); `Profile` holds mutable,
public-facing content. Rendering someone's profile never loads their
credentials, because those fields simply aren't there.

**Verification emails are sent after the transaction commits.** Registration
publishes an application event consumed by a `@TransactionalEventListener` at
`AFTER_COMMIT`, so a slow or failing mail provider can never roll back a
registration — and a rolled-back registration never sends an email.

**Polling for chat, not WebSockets.** A message table plus a periodic fetch is
indistinguishable from real-time at this scale, and it can be upgraded later
without touching the data model.

---

## Running locally

Requires JDK 21 and Docker.

```bash
git clone https://github.com/<user>/alma-matcher.git
cd alma-matcher

docker compose up -d      # PostgreSQL on :5432
./gradlew bootRun         # app on :8080
```

Registering a user:

```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"mario.rossi@studio.unibo.it","username":"mario_r",
       "password":"a-long-enough-password","firstName":"Mario",
       "lastName":"Rossi","birthDate":"2003-04-15"}'
```

No mail provider is wired in yet: the verification link is written to the
application log by `LoggingEmailSender`. Open it to activate the account.

Configuration lives in `src/main/resources/application.yaml` — database
connection, allowed email domains, username rules and token validity.

---

## Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Create an account. `201`, or `400`/`409` with a JSON error body. |
| `GET` | `/api/auth/verify?token=…` | Activate an account and redirect to a confirmation page. |

---

## Roadmap

- [x] Domain model — `Account`, `Profile`, `EmailVerificationToken`
- [x] Registration — domain check, duplicate detection, minimum age, BCrypt
- [x] Email verification — token issuance and confirmation
- [ ] Login and logout
- [ ] Flyway migrations
- [ ] Real email provider
- [ ] User profiles
- [ ] Events
- [ ] Matching
- [ ] Chat
- [ ] Frontend

---

## About

A personal learning project — the goal is to move from desktop Java to
server-side development. Not affiliated with or endorsed by the University of
Bologna.
