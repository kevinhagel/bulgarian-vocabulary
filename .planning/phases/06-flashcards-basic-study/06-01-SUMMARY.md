---
phase: 06-flashcards-basic-study
plan: 01
status: complete
completed: 2026-02-17
duration: ~8min
---

# Summary: Phase 06-01 — SRS Backend

## What was built

- **V5 migration** — 4 new tables: `srs_state`, `study_sessions`, `session_cards`, `study_reviews`
- **Enums** — `SessionStatus` (ACTIVE/COMPLETED/ABANDONED), `ReviewRating` (CORRECT/INCORRECT)
- **Domain entities** — `SrsState`, `StudySession`, `SessionCard` (composite PK), `StudyReview`
- **Repositories** — 4 Spring Data JPA repositories with JPQL queries
- **SM-2 algorithm** — `SrsAlgorithmService`: interval 0→1→6→prev×EF; INCORRECT resets interval to 1, lowers EF to min 1.30
- **Session service** — `StudySessionService`: start, next card, rate, end, due-count
- **REST API** — `StudyController` with 5 endpoints under `/api/study`

## Deviations from plan

- `SessionCardRepository.findFirstUnreviewed` uses `Pageable` parameter (returns `List<SessionCard>`) instead of `Optional<SessionCard>` — avoids `IncorrectResultSizeDataAccessException` when multiple unreviewed cards exist. Service wraps with a private `findFirstUnreviewed(Long)` helper returning `Optional`.
- JPQL enum comparisons use fully qualified class names (`com.vocab.bulgarian.domain.enums.Source.USER_ENTERED`) rather than string literals.
- `DEFAULT_MAX_CARDS` constant removed from service (unused — controller default handles it).
- `rateCard` auto-complete also sets `endedAt` on the session.

## Flyway note

`mvn flyway:migrate` must be run with `-Dflyway.url=jdbc:postgresql://localhost:5432/bulgarian_vocab` on Mac Studio (plugin defaults to `macstudio` hostname, intended for remote use from MacBook M2).

## Verification

- `mvn compile -q` passes cleanly
- Schema version: 5 (all V1–V5 applied)
