---
phase: 06-flashcards-basic-study
plan: 04
status: complete
completed: 2026-02-17
duration: ~4min
---

# Summary: Phase 06-04 — Error Handling & Polish

## What was built

- **GlobalExceptionHandler** — added `IllegalStateException` → 400 ProblemDetail handler (consistent with existing style)
- **VocabularyList** — `onNavigateStudy?` prop; imports `useDueCount`; orange Study badge in header when cards due
- **App.tsx** — passes `onNavigateStudy={() => setAppView('study')}` to VocabularyList

## UAT note

UAT deferred to user's return — backend needs restart to activate all new study endpoints (was running during build without them).
