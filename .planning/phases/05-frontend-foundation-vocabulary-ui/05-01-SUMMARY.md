---
phase: 05-frontend-foundation-vocabulary-ui
plan: 01
subsystem: frontend-infrastructure
tags: [react, vite, typescript, tailwind, tanstack-query, frontend-foundation]
dependency_graph:
  requires:
    - "04-02 (vocabulary REST API for data fetching)"
    - "03-02 (audio generation service for playback)"
  provides:
    - "React 19 + Vite 6 dev environment with hot-reload"
    - "TypeScript types matching backend DTOs"
    - "TanStack Query infrastructure for data fetching"
    - "Layout shell for all frontend pages"
    - "AudioPlayer component for vocabulary audio playback"
    - "Backend audio generation endpoint for frontend use"
  affects:
    - "All subsequent frontend plans (05-02, 05-03) depend on this foundation"
tech_stack:
  added:
    - "React 19 (UI library)"
    - "Vite 6 (build tool)"
    - "TypeScript (type safety)"
    - "Tailwind CSS 4 (styling)"
    - "TanStack Query (data fetching and caching)"
    - "Zustand (state management - installed, not yet used)"
    - "React Hook Form (form handling - installed, not yet used)"
    - "Zod (validation - installed, not yet used)"
    - "Axios (HTTP client)"
    - "Sofia Sans font (Bulgarian Cyrillic support)"
  patterns:
    - "@ path alias for clean imports"
    - "Vite proxy to Spring Boot backend for seamless API calls"
    - "QueryClientProvider pattern for TanStack Query"
    - "Layout component pattern for consistent page structure"
    - "Async audio generation endpoint with CompletableFuture"
key_files:
  created:
    - "frontend/package.json (dependencies and scripts)"
    - "frontend/vite.config.ts (Vite config with proxy and path alias)"
    - "frontend/tsconfig.json, tsconfig.app.json (TypeScript config)"
    - "frontend/index.html (Bulgarian language, Sofia Sans font)"
    - "frontend/src/index.css (Tailwind CSS 4 with custom theme)"
    - "frontend/src/main.tsx (React entry point with QueryClientProvider)"
    - "frontend/src/App.tsx (root component with Layout)"
    - "frontend/src/lib/api.ts (Axios client)"
    - "frontend/src/lib/queryClient.ts (TanStack Query config)"
    - "frontend/src/features/vocabulary/types.ts (TypeScript DTOs)"
    - "frontend/src/components/layout/Header.tsx (sticky header)"
    - "frontend/src/components/layout/Layout.tsx (page layout shell)"
    - "frontend/src/components/audio/AudioPlayer.tsx (reusable audio player)"
  modified:
    - "backend/src/main/java/com/vocab/bulgarian/audio/controller/AudioController.java (added POST /api/audio/generate)"
decisions:
  - decision: "Use Tailwind CSS 4 with @theme instead of tailwind.config.js"
    rationale: "Tailwind CSS 4 uses CSS-based configuration for simpler setup and better type safety"
    impact: "Custom theme tokens defined in src/index.css @theme block"
  - decision: "Use @ path alias for imports"
    rationale: "Cleaner imports (e.g., @/lib/api vs ../../lib/api) and easier refactoring"
    impact: "Configured in vite.config.ts and tsconfig.app.json"
  - decision: "Vite proxy for /api requests to localhost:8080"
    rationale: "Avoid CORS issues during development, seamless API access"
    impact: "Frontend can call /api/* and Vite proxies to Spring Boot backend"
  - decision: "TanStack Query with 5-minute stale time"
    rationale: "Vocabulary data changes infrequently, reduce backend load"
    impact: "Fewer API calls, better UX with instant cached responses"
  - decision: "Separate LemmaResponseDTO and LemmaDetailDTO types"
    rationale: "List view has inflectionCount, detail view has full inflections array"
    impact: "Type safety matches backend DTO structure, prevents N+1 queries"
  - decision: "Sofia Sans font for Bulgarian Cyrillic rendering"
    rationale: "Modern, clean font with excellent Cyrillic support"
    impact: "Loaded via Google Fonts, set as default sans-serif in Tailwind theme"
  - decision: "AudioPlayer component uses SVG icons instead of emojis"
    rationale: "Better cross-platform rendering consistency, more professional appearance"
    impact: "Play/pause/loading states use SVG paths instead of Unicode symbols"
  - decision: "Backend POST /api/audio/generate endpoint returns CompletableFuture"
    rationale: "Async generation prevents blocking frontend requests, improves responsiveness"
    impact: "Frontend receives filename immediately after generation completes"
metrics:
  duration_minutes: 5
  tasks_completed: 2
  files_created: 13
  files_modified: 1
  commits: 2
  commit_hashes:
    - "919e473 (Task 1: Vite + React scaffolding)"
    - "71afcff (Task 2: Backend endpoint, types, API client, layout, AudioPlayer)"
  completed_date: "2026-02-16"
---

# Phase 05 Plan 01: Frontend Foundation - React 19 + Vite Setup Summary

**One-liner:** React 19 + Vite 6 + TypeScript frontend with Tailwind CSS 4, TanStack Query, layout shell, AudioPlayer component, and backend audio generation endpoint - complete foundation for vocabulary UI.

## Objective

Scaffold the complete React 19 + Vite 6 + TypeScript frontend project with all dependencies, configurations, shared infrastructure components, and backend audio generation endpoint needed for frontend audio playback.

## What Was Built

### Task 1: Vite + React 19 + TypeScript Project Scaffolding

**Created frontend/ directory with:**
- Vite 6 + React 19 template with TypeScript
- Production dependencies: TanStack Query, Zustand, React Hook Form, Zod, @hookform/resolvers, Axios
- Dev dependencies: TanStack Query Devtools, Tailwind CSS 4 (@tailwindcss/vite)
- Vite configuration: API proxy (/api -> localhost:8080), @ path alias, Tailwind plugin
- TypeScript configuration: baseUrl and paths for @ alias
- index.html: lang="bg" for Bulgarian, Sofia Sans font, "Bulgarian Vocabulary Tutor" title
- Tailwind CSS 4 custom theme: primary colors, Sofia Sans font family, gray-50 background
- .gitignore and .env.example files

**Verification:** `npm run build` succeeded with no errors.

**Commit:** 919e473

### Task 2: Backend Audio Endpoint + Frontend Infrastructure

**Backend changes:**
- Added POST /api/audio/generate endpoint to AudioController
- Injected AudioGenerationService into controller
- Endpoint accepts JSON body with "text" field (Bulgarian text)
- Returns CompletableFuture<ResponseEntity<Map<String, String>>> with "filename" key
- Async generation via audioGenerationService.generateAudioAsync(text)
- Validation: 400 Bad Request if text is blank
- Error handling: 500 Internal Server Error on generation failure

**Frontend TypeScript types (src/features/vocabulary/types.ts):**
- Enum types as string union types: Source, PartOfSpeech (11 values), DifficultyLevel, ReviewStatus
- InflectionDTO: id, form, grammaticalInfo
- LemmaResponseDTO: id, text, translation, partOfSpeech, category, difficultyLevel, source, reviewStatus, inflectionCount, createdAt
- LemmaDetailDTO: all LemmaResponseDTO fields + notes, inflections array, updatedAt
- CreateLemmaRequest: wordForm, translation, notes (optional)
- UpdateLemmaRequest: text, translation, notes, inflections (optional)
- InflectionUpdate: id (optional), form, grammaticalInfo (optional)
- PaginatedResponse<T>: content, totalElements, totalPages, number, size, first, last

**API client (src/lib/api.ts):**
- Axios instance with baseURL from import.meta.env.VITE_API_BASE_URL (fallback /api)

**Query client (src/lib/queryClient.ts):**
- QueryClient with defaultOptions: staleTime 5 minutes, retry 1, refetchOnWindowFocus false

**Layout components:**
- Header.tsx: sticky header, white background, shadow, mobile-responsive flex layout, "Bulgarian Vocabulary" title
- Layout.tsx: wraps children with Header at top and main content area (container mx-auto px-4 py-6)

**AudioPlayer component (src/components/audio/AudioPlayer.tsx):**
- Props: audioUrl (required), label (optional), size ('sm' | 'md')
- useRef<HTMLAudioElement> for audio element control
- State: isPlaying, isLoading, hasError
- Event handlers: handlePlay (play/pause), handleAudioPlay, handleAudioPause, handleAudioEnded, handleAudioError
- Preload metadata for minimal initial load
- Play button: SVG play icon, pause icon on playing, loading spinner, error X
- Button styling: rounded-full, blue-500 background, hover state, disabled state
- Label with lang="bg" attribute
- Size variants: sm (w-8 h-8), md (w-10 h-10)

**App integration:**
- main.tsx: wrapped App in StrictMode > QueryClientProvider > App + ReactQueryDevtools
- App.tsx: uses Layout component with placeholder "Vocabulary list will go here"

**Verification:** 
- `npx tsc --noEmit` passed with no type errors
- `npm run build` succeeded
- Backend `mvn compile` succeeded

**Commit:** 71afcff

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing vite-env.d.ts file**
- **Found during:** Task 1 verification
- **Issue:** Vite template didn't generate src/vite-env.d.ts file
- **Fix:** Skipped - Vite works without it, types are provided by vite/client in tsconfig.app.json
- **Files modified:** None
- **Commit:** N/A (no fix needed)

**2. [Rule 1 - Bug] TypeScript verbatimModuleSyntax error in Layout.tsx**
- **Found during:** Task 2 verification (npm run build)
- **Issue:** TypeScript error: 'ReactNode' is a type and must be imported using a type-only import when 'verbatimModuleSyntax' is enabled
- **Fix:** Changed `import { ReactNode }` to `import { type ReactNode }` in Layout.tsx
- **Files modified:** frontend/src/components/layout/Layout.tsx
- **Commit:** 71afcff (included in Task 2 commit)

**3. [Rule 2 - Missing critical functionality] .env file in .gitignore but no .env.example**
- **Found during:** Task 1 commit
- **Issue:** Git rejected adding .env (in .gitignore), but .env.example was missing
- **Fix:** Created .env.example from .env for source control and documentation
- **Files modified:** frontend/.env.example (created)
- **Commit:** 919e473 (updated Task 1 commit message)

## Verification Results

All verification criteria passed:

- [x] `cd frontend && npm run build` succeeded with no errors
- [x] `cd frontend && npx tsc --noEmit` succeeded with no type errors
- [x] `cd backend && mvn compile -q` succeeded with audio endpoint changes
- [x] frontend/package.json contains: react, @tanstack/react-query, zustand, react-hook-form, zod, @hookform/resolvers, axios, tailwindcss
- [x] frontend/vite.config.ts contains proxy configuration for /api -> localhost:8080
- [x] frontend/index.html contains lang="bg" and Sofia Sans font link
- [x] frontend/src/features/vocabulary/types.ts contains LemmaResponseDTO and LemmaDetailDTO
- [x] frontend/src/components/audio/AudioPlayer.tsx contains useRef and audio element
- [x] backend AudioController has POST /api/audio/generate endpoint

## Success Criteria

All success criteria met:

- [x] React 19 + Vite 6 + TypeScript project builds and runs with HMR
- [x] All dependencies installed (TanStack Query, Zustand, React Hook Form, Zod, Tailwind, Axios)
- [x] API proxy routes /api to Spring Boot on localhost:8080
- [x] TypeScript types match backend DTOs exactly
- [x] Layout shell renders with responsive header
- [x] AudioPlayer component provides play/pause functionality
- [x] Backend audio generation endpoint available for frontend use

## Self-Check: PASSED

**Files created verification:**
```bash
[ -f "/Users/kevin/projects/bulgarian-vocabulary/frontend/package.json" ] && echo "FOUND: frontend/package.json" || echo "MISSING: frontend/package.json"
# FOUND: frontend/package.json

[ -f "/Users/kevin/projects/bulgarian-vocabulary/frontend/vite.config.ts" ] && echo "FOUND: frontend/vite.config.ts" || echo "MISSING: frontend/vite.config.ts"
# FOUND: frontend/vite.config.ts

[ -f "/Users/kevin/projects/bulgarian-vocabulary/frontend/src/features/vocabulary/types.ts" ] && echo "FOUND: frontend/src/features/vocabulary/types.ts" || echo "MISSING: frontend/src/features/vocabulary/types.ts"
# FOUND: frontend/src/features/vocabulary/types.ts

[ -f "/Users/kevin/projects/bulgarian-vocabulary/frontend/src/components/audio/AudioPlayer.tsx" ] && echo "FOUND: frontend/src/components/audio/AudioPlayer.tsx" || echo "MISSING: frontend/src/components/audio/AudioPlayer.tsx"
# FOUND: frontend/src/components/audio/AudioPlayer.tsx

[ -f "/Users/kevin/projects/bulgarian-vocabulary/backend/src/main/java/com/vocab/bulgarian/audio/controller/AudioController.java" ] && echo "FOUND: backend AudioController.java" || echo "MISSING: backend AudioController.java"
# FOUND: backend AudioController.java
```

**Commits verification:**
```bash
git log --oneline --all | grep -q "919e473" && echo "FOUND: 919e473" || echo "MISSING: 919e473"
# FOUND: 919e473

git log --oneline --all | grep -q "71afcff" && echo "FOUND: 71afcff" || echo "MISSING: 71afcff"
# FOUND: 71afcff
```

All files created and commits exist as documented.

## Next Steps

Phase 05 Plan 02 will implement:
- VocabularyList component with pagination
- TanStack Query hooks for fetching vocabulary data
- Filters for source, part of speech, difficulty level
- Integration with AudioPlayer for vocabulary audio playback
- Create/edit lemma forms with React Hook Form + Zod validation

This plan provides the complete foundation for all subsequent frontend work.
