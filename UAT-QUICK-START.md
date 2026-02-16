# UAT Quick Start Guide

**Date**: 2026-02-16
**Project**: Bulgarian Vocabulary Tutor
**Environment**: Mac Studio (all localhost)

---

## 🚀 Quick Start (One Command)

```bash
./start-uat.sh
```

This will:
1. ✓ Start Docker infrastructure (PostgreSQL, Valkey, pgAdmin)
2. ✓ Start Spring Boot backend on port 8080
3. ✓ Start React frontend on port 5173
4. ✓ Verify all connections

**Then open**: http://localhost:5173

---

## 🔧 Manual Start (Step-by-step)

### 1. Start Infrastructure
```bash
docker-compose up -d
docker-compose ps  # Verify all healthy
```

### 2. Set Environment Variables
```bash
export POSTGRES_PASSWORD='BG_Vocab_2026_PostgreSQL!'
export OLLAMA_BASE_URL='http://localhost:11434'
export AUDIO_STORAGE_PATH='/Volumes/T9-NorthStar/bulgarian-vocab/audio'
```

### 3. Start Backend
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Wait for: `Started BulgarianVocabularyApplication`

### 4. Start Frontend (new terminal)
```bash
cd frontend
npm run dev
```

Wait for: `Local: http://localhost:5173/`

---

## 🧪 What to Test

### Backend API (Phase 1-4)
- **Health Check**: http://localhost:8080/actuator/health
- **List Vocabulary**: http://localhost:8080/api/vocabulary
- **API Docs**: Check CRUD operations work

### Frontend (Phase 5)
- **Home Page**: http://localhost:5173
- **Vocabulary List**: Should show entries with Bulgarian text
- **Search/Filter**: Test search by Bulgarian word
- **Create Entry**: Click "Add Vocabulary" button
- **Audio Playback**: Click speaker icons

---

## ✅ Expected Behavior

### Working Features (Phases 1-4 Complete)
- ✓ Database with lemmas and inflections
- ✓ LLM integration (lemma detection, inflection generation)
- ✓ TTS audio generation (Edge TTS)
- ✓ REST API for vocabulary CRUD
- ✓ Reference data seeded (interrogatives, pronouns, etc.)

### Frontend Status (Phase 5 - In Progress)
- ⚠️  UI components exist but may have issues
- ⚠️  API connectivity should work via proxy
- ⚠️  Audio playback may need testing

### Known Limitations
- ✗ No STT (by design)
- ✗ No multi-word phrase auto-detection (by design)
- ✗ No flashcards yet (Phase 6)
- ✗ No word lists yet (Phase 7)
- ✗ No spaced repetition yet (Phase 6-8)

---

## 🐛 Troubleshooting

### Backend won't start
```bash
# Check logs
cat backend-uat.log

# Common issue: Docker Compose not running
docker-compose ps
docker-compose up -d

# Check PostgreSQL
PGPASSWORD='BG_Vocab_2026_PostgreSQL!' psql -h localhost -U vocab_user -d bulgarian_vocab -c "SELECT 1;"
```

### Frontend shows errors
```bash
# Check logs
cat frontend-uat.log

# Check if backend is running
curl http://localhost:8080/actuator/health

# Check Vite config proxy
cat frontend/vite.config.ts
```

### LLM features don't work
```bash
# Check Ollama
curl http://localhost:11434/api/tags

# Check if BgGPT model exists
curl http://localhost:11434/api/tags | grep bggpt

# If not, pull the model
ollama pull todorov/bggpt:9b
```

### Audio generation fails
```bash
# Check Edge TTS installed
which edge-tts

# Check audio storage path
ls -la /Volumes/T9-NorthStar/bulgarian-vocab/audio

# If doesn't exist, create it
mkdir -p /Volumes/T9-NorthStar/bulgarian-vocab/audio
```

---

## 🛑 Stop Services

```bash
# Find process IDs
ps aux | grep "spring-boot:run"
ps aux | grep "vite"

# Kill processes
kill <BACKEND_PID>
kill <FRONTEND_PID>

# Stop Docker
docker-compose stop
```

Or use the stop script:
```bash
./stop-dev.sh
```

---

## 📊 Service URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:5173 | - |
| Backend API | http://localhost:8080 | - |
| pgAdmin | http://localhost:5050 | admin@example.com / BG_Vocab_2026_PgAdmin! |
| Redis Commander | http://localhost:8082 | admin / admin123 |
| Ollama | http://localhost:11434 | - |

---

## 📋 Full UAT Checklist

See: `UAT-CHECKLIST.md` for comprehensive testing scenarios

---

## 🆘 Need Help?

1. Check logs: `backend-uat.log` and `frontend-uat.log`
2. Check Docker: `docker-compose ps`
3. Check processes: `ps aux | grep -E "spring-boot|vite"`
4. Review error messages in browser console (F12)

---

**Last Updated**: 2026-02-16
