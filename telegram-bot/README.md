# Kevin Mac Studio — Telegram Bot

**Bot:** @HagelMacStudio_bot
**Purpose:** Bulgarian vocabulary companion + Mac Studio infrastructure watchdog
**Runs as:** launchd service on Mac Studio (outside Docker, so it can monitor everything else)

---

## Quick Reference

### Learning Commands

| Command | What it does |
|---------|-------------|
| `/word казвам се` | Look up any Bulgarian word or phrase |
| `/word hello` | Look up by English word |
| `казвам се` | Just type the word — no slash needed |
| `/stats` | Your vocabulary counts, SRS cards due, retention rate |
| `/queue` | Sentence generation progress bar |
| `/lesson_today` | Send a pre-lesson brief right now (vocab count, sentences, SRS due) |
| `/retry_failed` | Requeue all failed or stuck vocabulary words for reprocessing |

### Infrastructure Commands

| Command | What it does |
|---------|-------------|
| `/status` | Full system health: Colima, containers, Vault, nginx, Ollama, disk, app stats |
| `/containers` | Docker container list with health status |
| `/disk` | Disk usage for Internal SSD, T7-NorthStar (Ollama), T9-NorthStar (DB) |
| `/ollama` | Which model is loaded and whether it's on GPU or CPU |
| `/vault` | Vault seal status |
| `/logs backend` | Last 30 lines of backend container logs |
| `/logs postgres` | Last 30 lines of any container (use the name from /containers) |
| `/cache_clear` | Clear the Valkey LLM response cache |
| `/restart colima` | Stop and restart the Colima VM (~30s, waits for confirmation) |
| `/restart backend` | Restart the Spring Boot container |
| `/restart all` | Run `docker compose up -d` (brings up anything that's down) |
| `/help` | Show this command list in the chat |

---

## Automatic Alerts (no action needed)

The bot monitors in the background and messages you proactively:

| What | When | Message |
|------|------|---------|
| Colima down | within 5 min | 🔴 Alert + auto-restart attempt |
| Container unhealthy | within 5 min | 🔴 Alert with restart suggestion |
| Vault sealed | within 5 min | 🔴 Alert (auto-unseal plist usually fixes within 30s) |
| Disk > 80% | within 1 hour | 🟡 Warning |
| Disk > 90% | within 1 hour | 🔴 Critical |
| SSL cert < 30 days | 9am daily | 🟡 Warning |
| SSL cert < 7 days | 9am daily | 🔴 Critical |

**Quiet hours:** No proactive alerts between midnight and 6am.

---

## Scheduled Messages

| Time | Message |
|------|---------|
| 8:00am daily | Morning message — word of the day, SRS cards due, new words |
| 6:00pm daily | SRS nudge — only sent if 5 or more cards are due |
| Sunday 8:00pm | Weekly summary — words added, reviews done, retention rate |

---

## Service Management

**Check it's running:**
```bash
launchctl list | grep telegram
# Column 1 = PID (non-zero means running)
```

**Watch live logs:**
```bash
tail -f ~/bulgarian-vocabulary/telegram-bot/bot.log
```

**Restart after a code change:**
```bash
launchctl unload ~/Library/LaunchAgents/com.bgvocab.telegram-bot.plist
launchctl load  ~/Library/LaunchAgents/com.bgvocab.telegram-bot.plist
```

**Log file:** `telegram-bot/bot.log` (rotated by launchd on each restart)

---

## Configuration

All settings are in `telegram-bot/.env` (gitignored — never committed):

```
BOT_TOKEN=...        # From @BotFather
CHAT_ID=...          # Your Telegram user ID
BACKEND_URL=http://localhost:8080
REDIS_HOST=localhost
REDIS_PORT=6379
```

Lesson schedule, quiet hours, and job times are in `config.py`.

---

## Files

```
telegram-bot/
├── bot.py            Main bot — commands, background monitors, scheduler
├── checks.py         Shell-level health checks (Colima, Docker, Vault, disk, SSL, Ollama)
├── vocab.py          Spring Boot API client (word lookup, stats, cache, retry)
├── scheduler.py      Scheduled messages (morning word, SRS nudge, weekly summary)
├── config.py         Loads .env and defines schedule times
├── requirements.txt  Python dependencies
├── .env              Credentials (gitignored)
└── .env.example      Template showing required variables
```
