---
phase: 06-flashcards-basic-study
plan: 03
status: complete
completed: 2026-02-17
duration: ~5min
---

# Summary: Phase 06-03 — Progress Dashboard

## What was built

- **ProgressDashboardDTO**, **LemmaStatsDTO** records
- **ProgressService** — getDashboard() aggregates across all repos; getLemmaStats() merges review + SRS data
- **ProgressController** — GET /api/study/progress, GET /api/study/stats/{lemmaId}
- **LemmaRepository.countBySource** added (Spring Data derived query)
- **useProgress**, **useLemmaStats** hooks (both `retry: false`)
- **ProgressDashboard** — 4-tile grid (vocab, sessions, reviewed, retention) + studied-words progress bar
- **LemmaSrsInfo** — study stats panel for USER_ENTERED lemmas; "Not studied yet" when reviewCount=0
- **VocabularyDetail** — renders LemmaSrsInfo below inflections for USER_ENTERED lemmas
- **App.tsx** — ProgressDashboard rendered below StudyLauncher in idle phase
