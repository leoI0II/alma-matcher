# AlmaMatcher

A closed social platform for University of Bologna students — meet people and
organise events, with registration restricted to verified university email
addresses.

> **Status: early development.** Nothing is functional yet. The project skeleton
> is in place; the domain model is being designed.

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
| Database | PostgreSQL |
| Build | Gradle (Kotlin DSL) |
| Frontend | not decided — mobile-first PWA planned |

### Some deliberate decisions

**Session-based auth, not JWT.** This is a single monolith with a browser
client. Sessions are the Spring Security default and, more importantly, can be
revoked instantly — a banned account loses access immediately rather than
whenever its token happens to expire.

**Polling for chat, not WebSockets.** A message table plus a periodic fetch is
indistinguishable from real-time at this scale, and it can be upgraded later
without touching the data model.

**Flyway from the first commit.** Schema changes are versioned, never applied
implicitly by Hibernate.

---

## Running locally

Requires JDK 21 and a running PostgreSQL instance.

```bash
git clone https://github.com/<user>/alma-matcher.git
cd alma-matcher
./gradlew bootRun
```

Database connection settings go in `src/main/resources/application.yml`.

---

## Roadmap

- [x] Project skeleton
- [ ] Domain model
- [ ] Database setup and migrations
- [ ] Registration with email verification
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
