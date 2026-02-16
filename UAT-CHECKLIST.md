# UAT Checklist - Bulgarian Vocabulary Tutor
**Date**: 2026-02-16
**Environment**: Mac Studio (localhost)
**Testing Phases 1-4**: Backend API

---

## Prerequisites ✓

- [x] Docker infrastructure running (PostgreSQL, Valkey, pgAdmin)
- [ ] Backend Spring Boot application running
- [ ] Frontend React dev server running
- [ ] Ollama with BgGPT model available

---

## Phase 1: Infrastructure Verification

### Docker Services
- [ ] PostgreSQL healthy on `localhost:5432`
- [ ] Valkey healthy on `localhost:6379`
- [ ] pgAdmin accessible at `http://localhost:5050`
- [ ] Can connect to PostgreSQL with credentials

### Database Schema
- [ ] Flyway migrations applied successfully
- [ ] `lemmas` table exists with correct structure
- [ ] `inflections` table exists with correct structure
- [ ] PGroonga extension installed
- [ ] Reference data seeded (interrogatives, pronouns, etc.)

**Test Commands**:
```bash
docker-compose ps
PGPASSWORD='BG_Vocab_2026_PostgreSQL!' psql -h localhost -U vocab_user -d bulgarian_vocab -c "\dt"
PGPASSWORD='BG_Vocab_2026_PostgreSQL!' psql -h localhost -U vocab_user -d bulgarian_vocab -c "SELECT COUNT(*) FROM lemmas;"
```

---

## Phase 2: LLM Integration

### Ollama Connectivity
- [ ] Ollama accessible at `http://localhost:11434`
- [ ] BgGPT model available (`todorov/bggpt:9b`)
- [ ] Spring Boot can connect to Ollama

### LLM Services
- [ ] Lemma detection service works
- [ ] Inflection generation service works
- [ ] Metadata generation service works
- [ ] Circuit breaker configured correctly
- [ ] Valkey caching works (check logs for cache hits)

**Test Commands**:
```bash
curl http://localhost:11434/api/tags
curl http://localhost:8080/actuator/health
```

---

## Phase 3: TTS Audio Generation

### Edge TTS
- [ ] edge-tts installed on Mac Studio
- [ ] Audio storage directory exists: `/Volumes/T9-NorthStar/bulgarian-vocab/audio`
- [ ] Can generate audio for Bulgarian text
- [ ] Audio files cached correctly

### Audio API
- [ ] POST to generate audio works
- [ ] GET `/api/audio/{filename}` serves audio files
- [ ] Path traversal protection works (try `../../../etc/passwd`)

**Test Commands**:
```bash
which edge-tts
ls -la /Volumes/T9-NorthStar/bulgarian-vocab/audio
curl -X POST http://localhost:8080/api/audio/generate -H "Content-Type: application/json" -d '{"text":"здравей","voice":"bg-BG-KalinaNeural"}'
```

---

## Phase 4: Core Vocabulary Management

### CRUD Operations

#### Create Vocabulary Entry
- [ ] Enter single Bulgarian word (any inflection) → LLM detects lemma
- [ ] Enter multi-word lemma explicitly (e.g., "казвам се")
- [ ] LLM auto-generates inflections
- [ ] LLM auto-generates part of speech
- [ ] LLM auto-generates category
- [ ] LLM auto-generates difficulty level
- [ ] English translation field required
- [ ] Notes field optional
- [ ] Audio generated for lemma
- [ ] Audio generated for all inflections

**Test API**:
```bash
curl -X POST http://localhost:8080/api/vocabulary \
  -H "Content-Type: application/json" \
  -d '{
    "inputText": "пиша",
    "englishTranslation": "to write",
    "notes": "Common verb"
  }'
```

#### Read/List Vocabulary
- [ ] GET `/api/vocabulary` returns paginated list
- [ ] Search by lemma text works (Bulgarian Cyrillic)
- [ ] Filter by part of speech works
- [ ] Filter by category works
- [ ] Filter by difficulty works
- [ ] Filter by source (user/system) works
- [ ] Sorting works correctly

**Test API**:
```bash
curl http://localhost:8080/api/vocabulary
curl http://localhost:8080/api/vocabulary?search=пиша
curl http://localhost:8080/api/vocabulary?partOfSpeech=VERB
```

#### Update Vocabulary Entry
- [ ] Can edit lemma text
- [ ] Can edit English translation
- [ ] Can edit notes
- [ ] Can edit inflections manually
- [ ] Changes persist correctly

**Test API**:
```bash
curl -X PUT http://localhost:8080/api/vocabulary/1 \
  -H "Content-Type: application/json" \
  -d '{
    "lemmaText": "пиша",
    "englishTranslation": "to write (updated)",
    "notes": "Updated notes"
  }'
```

#### Delete Vocabulary Entry
- [ ] DELETE removes entry from database
- [ ] Associated inflections also deleted (cascade)
- [ ] 404 returned for non-existent ID

**Test API**:
```bash
curl -X DELETE http://localhost:8080/api/vocabulary/999
```

### Text Detection
- [ ] Paste Bulgarian text → extracts single-word lemmas
- [ ] Multi-word phrases NOT auto-detected (by design)
- [ ] Cyrillic text processed correctly

---

## Phase 5: Frontend Foundation (Current Phase)

### React Application
- [ ] `npm run dev` starts Vite dev server
- [ ] Frontend accessible at `http://localhost:5173`
- [ ] No console errors on load
- [ ] TanStack Query configured
- [ ] Zustand store configured
- [ ] Axios API client works

### Vocabulary List Page
- [ ] Displays list of vocabulary entries
- [ ] Bulgarian Cyrillic renders correctly (large, clear fonts)
- [ ] Search/filter UI works
- [ ] Pagination works
- [ ] Audio play button on each entry
- [ ] Click entry to view details

### CRUD UI
- [ ] "Add Vocabulary" button opens modal
- [ ] Create form has validation (Zod + React Hook Form)
- [ ] Edit button opens edit modal
- [ ] Delete button shows confirmation dialog
- [ ] All mutations update list via TanStack Query

### Vocabulary Detail Page
- [ ] Shows lemma details
- [ ] Shows English translation
- [ ] Shows all inflections in table
- [ ] Audio playback for lemma
- [ ] Audio playback for each inflection
- [ ] "Back to list" button works

### Mobile Responsiveness
- [ ] Works on phone screen size
- [ ] Works on tablet screen size
- [ ] Works on laptop screen size

---

## Known Issues / Limitations

- [ ] STT (speech-to-text) not implemented (by design)
- [ ] Multi-word phrase auto-detection not implemented (by design)
- [ ] Fill-in-the-blank exercises not implemented (Phase 6+)
- [ ] Flashcards not implemented (Phase 6)
- [ ] Word lists not implemented (Phase 7)
- [ ] Spaced repetition not implemented (Phase 6-8)

---

## Performance Testing

- [ ] Create 10 vocabulary entries → acceptable response time
- [ ] Create 100 vocabulary entries → acceptable response time
- [ ] Search with 100+ entries → acceptable response time
- [ ] LLM response cached correctly (check logs)
- [ ] Audio generation happens in background (non-blocking)

---

## Security Testing

- [ ] SQL injection attempts fail
- [ ] Path traversal attempts fail on audio endpoint
- [ ] XSS attempts sanitized
- [ ] CORS configured correctly for frontend

---

## Notes

**Blockers**:
- Backend startup issue: Spring Boot Docker Compose integration looking in wrong directory (FIXED)

**Workarounds**:
- Disabled Spring Boot Docker Compose auto-start (`spring.docker.compose.enabled=false`)
- Run `docker-compose up -d` manually before starting backend

**Environment Variables Required**:
```bash
export POSTGRES_PASSWORD='BG_Vocab_2026_PostgreSQL!'
export OLLAMA_BASE_URL='http://localhost:11434'
export AUDIO_STORAGE_PATH='/Volumes/T9-NorthStar/bulgarian-vocab/audio'
```

---

## Sign-off

- [ ] All Phase 1-4 backend features tested and working
- [ ] All Phase 5 frontend features tested and working
- [ ] Ready to proceed to Phase 6 (Flashcards)

**Tested by**: _______________
**Date**: _______________
