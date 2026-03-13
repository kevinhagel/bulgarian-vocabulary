# Bulgarian Vocabulary Tutor — Project Docs

This is the Obsidian vault for the Bulgarian Vocabulary Tutor project.

## Structure

```
docs/
├── session-summaries/          ← Per-session dev logs
│   ├── _template.md            ← Copy this for new sessions
│   ├── 2026-02-16.md           ← Phase 5 completion, Mac Studio migration
│   └── 2026-02-17.md           ← Phase 6 start, iPhone access regression
└── README.md                   ← This file
```

## GSD Planning Files

Planning lives in `.planning/` at the project root (not in this vault):
- `.planning/PROJECT.md` — Project definition, requirements, decisions
- `.planning/ROADMAP.md` — All phases (1-8) with success criteria
- `.planning/STATE.md` — Current position, velocity, accumulated decisions
- `.planning/phases/` — Per-plan summaries after completion

## Quick Reference

| Item | Location |
|------|----------|
| Current phase | `.planning/STATE.md` |
| All phases | `.planning/ROADMAP.md` |
| Session history | `docs/session-summaries/` |
| App instructions | `CLAUDE.md` |

## Services (Mac Studio, all local)

| Service | Port | Notes |
|---------|------|-------|
| Spring Boot backend | 8080 | `mvn spring-boot:run -Dspring-boot.run.profiles=dev` |
| React frontend | 5173 | `npm run dev` |
| PostgreSQL | 5432 | Docker |
| Valkey (Redis) | 6379 | Docker |
| Ollama (BgGPT) | 11434 | Native |
| pgAdmin | 5050 | Docker |
