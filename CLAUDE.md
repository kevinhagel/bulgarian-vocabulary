# Project Instructions for Claude

## Architecture - CRITICAL

### Development Environment - Mac Studio (Everything Local)

**All development and infrastructure runs on Mac Studio**

- **Mac Studio M4 Max** (32 GPU cores, Metal 4): Everything runs here locally
  - Docker Compose for infrastructure (PostgreSQL, pgAdmin, Valkey)
  - Spring Boot backend development and execution
  - React frontend development (Phase 5+)
  - Ollama for LLM (BgGPT model) — auto-starts via launchd, no manual start needed
  - Claude Code and GSD workflow
  - No SSH tunnels needed — everything is localhost

### Development Workflow

**On Mac Studio (everything local):**
- Edit code in `backend/` (Spring Boot) and `frontend/` (React)
- Run Spring Boot via Maven: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- Access services via localhost: PostgreSQL (5432), Valkey (6379), Ollama (11434)
- Spring Boot connects to: `localhost:5432`, `localhost:6379`, `localhost:11434`
- Actuator endpoints: `http://localhost:8080/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`

### Startup

**Infrastructure** (PostgreSQL, Valkey, pgAdmin) — usually already running:
```bash
cd ~/bulgarian-vocabulary
docker-compose up -d
docker-compose ps
```

**Ollama** — auto-starts on login via launchd. If not running:
```bash
brew services restart ollama
ollama list    # verify models are found (stored on /Volumes/T7-NorthStar/ollama-models)
ollama ps      # check if a model is loaded and which processor
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
mvn spring-boot:run
# No env var exports needed — secrets come from Vault (auto-managed by launchd)
# Vault must be running and unsealed; check with: vault status
```

**Run React frontend (Phase 5+):**
```bash
cd ~/bulgarian-vocabulary/frontend
npm install              # Install dependencies (first time only)
npm run dev              # Start dev server (binds to 0.0.0.0:5173)
```

## Project Structure

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
│   ├── STATE.md
│   └── phases/
│
├── docs/                               ← Obsidian vault (session summaries)
│
├── config/                             ← Configuration files
│   ├── valkey.conf
│   └── valkey-prod.conf
│
├── docker-compose.yml                  ← Infrastructure services
├── .env                                ← Local env vars (gitignored, contains passwords)
└── CLAUDE.md                           ← This file
```

## Technology Stack

- **Backend**: Spring Boot 3.4.2, Java 25, Spring AI 1.1.0-M3, JPA/Hibernate
- **Async**: Java 25 virtual threads (`spring.threads.virtual.enabled=true`) — no thread pools
- **Database**: PostgreSQL 16 with PGroonga (Bulgarian Cyrillic full-text search)
- **Cache**: Valkey 7.2 (Redis-compatible) for LLM response caching
- **LLM**: Ollama (BgGPT: `todorov/bggpt:9B-IT-v1.0.Q4_K_M`) — models on T7 SSD
- **TTS**: edge-tts for Bulgarian audio generation
- **Frontend**: React 19 + Vite 6 + TypeScript
- **Metrics**: Spring Boot Actuator + Micrometer Prometheus

## Network (Mac Studio)

- IP: `192.168.1.10` (static via DHCP manual address)
- Router forwards ports 80, 443, 5170–5179 → Mac Studio
- Frontend externally accessible: `https://hagelbg.dyndns-ip.com` (via nginx TLS termination)
- nginx config: `/opt/homebrew/etc/nginx/servers/bulgarian-vocab.conf`
- TLS cert: `/etc/letsencrypt/live/hagelbg.dyndns-ip.com/` (Let's Encrypt, auto-renews via cron)
- Vite config: `host: 0.0.0.0`, allowedHosts includes DynDNS hostname

## Ollama Configuration

- **Models stored**: `/Volumes/T7-NorthStar/ollama-models` (external SSD)
- **Auto-start**: launchd via `~/Library/LaunchAgents/homebrew.mxcl.ollama.plist`
- **Env vars**: set via `~/Library/LaunchAgents/ollama-environment.plist` (survives brew updates)
- **GPU**: 76% GPU / 24% CPU on M4 Max — `num-gpu: 99` in application-dev.yml
- **No menu bar icon**: Homebrew CLI install. For icon, install Ollama.app from ollama.ai

## Bulgarian Language Requirements

- **Lemma definition**: 1st person singular for verbs (Bulgarian has no infinitive)
- **Domain model**: Lemma entity as canonical form with Inflection entities
- **Multi-word lemmas**: Support phrases like "казвам се", "искам да"
- **Full-text search**: PGroonga for Bulgarian Cyrillic support

## Environment Variables

The `.env` file (gitignored) is auto-loaded by docker-compose and contains all passwords.
Backend picks up `POSTGRES_PASSWORD` from the shell environment set at container init time.

For Flyway Maven plugin (run from CLI):
```bash
export POSTGRES_PASSWORD='BG_Vocab_2026_PostgreSQL!'
mvn flyway:info
```

Ollama env vars are set in `~/Library/LaunchAgents/ollama-environment.plist` — do not set
them in `.zshrc` only, as launchd won't see them.

### Google OAuth2 secrets — now in Vault

Google OAuth2 secrets are stored in Vault (`secret/bulgarian-vocabulary`) and loaded
automatically at startup. No manual exports needed.

**Vault path**: `secret/bulgarian-vocabulary`
**Keys**: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `ALLOWED_EMAIL_KEVIN`, `ALLOWED_EMAIL_HUW`, `ALLOWED_EMAIL_ELENA`

To inspect: `vault kv get secret/bulgarian-vocabulary`
To update: `vault kv patch secret/bulgarian-vocabulary KEY=VALUE`

Google Console: https://console.cloud.google.com → APIs & Services → Credentials
Redirect URI: `https://hagelbg.dyndns-ip.com/login/oauth2/code/google`

### Vault

- **Server**: auto-starts via `~/Library/LaunchAgents/vault.plist` (launchd, KeepAlive)
- **Auto-unseal**: `~/Library/LaunchAgents/vault-unseal.plist` (runs every 30s, reads `~/.vault-unseal-key`)
- **Config**: `/opt/homebrew/etc/vault/config.hcl` (file storage, 127.0.0.1:8200)
- **UI**: http://localhost:8200/ui
- **Token**: stored in `backend/secrets.properties` (gitignored)
- **Unseal key**: `~/.vault-unseal-key` (chmod 600) — also in Dashlane

```bash
vault status                             # Check sealed/unsealed
vault kv get secret/bulgarian-vocabulary # Inspect secrets
```

## Never Do This

- ❌ Run docker-compose on a different machine
- ❌ Expose PostgreSQL/Valkey to internet (127.0.0.1 binding only)
- ❌ Commit .env or .env.production files (contain passwords)
- ❌ Commit hardcoded passwords anywhere (use `${env.VAR}` in Maven, `${VAR}` in Spring)
- ❌ Modify Phase 1-4 database schema (migrations are immutable)
- ❌ Use Python in scripts (not in tech stack — use jq, ollama CLI, curl)
- ❌ Use `brew services restart ollama` without knowing it resets the plist
  (the ollama-environment.plist re-applies env vars, so it's safe — but good to know)
