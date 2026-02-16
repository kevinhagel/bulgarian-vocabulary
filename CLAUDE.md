# Project Instructions for Claude

## Architecture - CRITICAL

### Development Environment - Mac Studio (Everything Local)

**All development and infrastructure runs on Mac Studio**

- **Mac Studio**: Everything runs here locally
  - Docker Compose for infrastructure (PostgreSQL, pgAdmin, Valkey)
  - Spring Boot backend development and execution
  - React frontend development (Phase 5+)
  - Ollama for LLM (BgGPT model)
  - Claude Code and GSD workflow
  - No SSH tunnels needed - everything is localhost

### Development Workflow

**On Mac Studio (everything local):**
- Edit code in `backend/` (Spring Boot) and `frontend/` (React)
- Run Spring Boot via IDE/IntelliJ or Maven: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- Access services via localhost: PostgreSQL (5432), Valkey (6379), Ollama (11434)
- Spring Boot connects to: `localhost:5432`, `localhost:6379`, `localhost:11434`

### Deployment

**Infrastructure management:**
```bash
# Start infrastructure
cd ~/bulgarian-vocabulary
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f postgres
```

**Database migrations:**
```bash
cd ~/bulgarian-vocabulary/backend
export POSTGRES_PASSWORD='BG_Vocab_2026_PostgreSQL!'
mvn flyway:migrate        # Execute migrations
mvn flyway:info           # Check migration status
```

**Run Spring Boot backend:**
```bash
cd ~/bulgarian-vocabulary/backend
export POSTGRES_PASSWORD='BG_Vocab_2026_PostgreSQL!'
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Run React frontend (Phase 5+):**
```bash
cd ~/bulgarian-vocabulary/frontend
npm install              # Install dependencies
npm run dev              # Start dev server
npm run build            # Build for production
```

## Project Structure

**Single-machine development with all components:**

```
bulgarian-vocabulary/                    ← Project root
├── backend/                            ← Spring Boot application
│   ├── src/                            ← Java source code
│   ├── pom.xml                         ← Maven configuration
│   └── target/                         ← Build output
│
├── frontend/                           ← React application
│   ├── src/
│   ├── package.json
│   └── (React/Vite structure)
│
├── .planning/                          ← GSD workflow files
│   ├── PROJECT.md
│   ├── ROADMAP.md
│   └── phases/
│
├── config/                             ← Configuration files
│   ├── valkey.conf
│   └── valkey-prod.conf
│
├── scripts/                            ← Utility scripts
│
├── docker-compose.yml                  ← Infrastructure services
├── .env.production                     ← Environment variables
└── CLAUDE.md                           ← This file
```

## Technology Stack

- **Backend**: Spring Boot 4.2.x, Java 25, Spring AI 2.0, JPA/Hibernate
- **Database**: PostgreSQL 16 with PGroonga (Bulgarian Cyrillic full-text search)
- **Cache**: Valkey 7.2 (Redis-compatible) for LLM response caching
- **LLM**: Ollama (BgGPT model: todorov/bggpt:9b) running locally
- **TTS**: edge-tts 7.2.7 for Bulgarian audio generation
- **Frontend**: React 19 + Vite 6 + TypeScript

## Bulgarian Language Requirements

- **Lemma definition**: 1st person singular for verbs (Bulgarian has no infinitive)
- **Domain model**: Lemma entity as canonical form with Inflection entities
- **Multi-word lemmas**: Support phrases like "казвам се", "искам да"
- **Full-text search**: PGroonga for Bulgarian Cyrillic support

## Environment Variables

Set these before running backend:
```bash
export POSTGRES_PASSWORD='BG_Vocab_2026_PostgreSQL!'
export OLLAMA_BASE_URL='http://localhost:11434'
export AUDIO_STORAGE_PATH='/Volumes/T9-NorthStar/bulgarian-vocab/audio'
```

## Never Do This

- ❌ Run docker-compose on a different machine
- ❌ Expose PostgreSQL/Valkey to internet (127.0.0.1 binding only)
- ❌ Commit .env or .env.production files (contain passwords)
- ❌ Modify Phase 1-4 database schema (migrations are immutable)
