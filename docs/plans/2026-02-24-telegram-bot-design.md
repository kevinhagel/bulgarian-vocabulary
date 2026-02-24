# Telegram Bot Design — Bulgarian Vocabulary Mac Studio
**Date:** 2026-02-24
**Status:** Brainstorming / pre-approval
**Author:** Kevin Hagel + Claude

---

## Executive Summary

A standalone Python bot running as a launchd service on the Mac Studio. It handles two distinct roles:

1. **Learning companion** — daily vocabulary reminders, word-of-the-day, pre-lesson briefs, SRS nudges, quick word lookup
2. **Infrastructure watchdog** — monitors Colima, Docker containers, disk space, Vault, nginx; alerts proactively and accepts commands to restart services

The bot is the only thing on the Mac Studio that runs *outside* Docker Compose, so it can monitor and fix everything else.

---

## Part 1 — Your Sleep and Colima Question

### Short Answer

**Your Mac Studio is already correctly configured — it will not sleep.**

Running `pmset -g` shows:
```
sleep        0   ← system sleep is DISABLED
disksleep    0   ← disk sleep is DISABLED
standby      0   ← standby is DISABLED
displaysleep 30  ← only your monitors turn off (correct)
womp         1   ← Wake on LAN is ENABLED
autorestart  1   ← auto-restarts after power failure
```

`sleep 0` is the critical setting. It means the Mac Studio stays fully awake even after the displays go dark. Colima should not die from sleep.

### Then Why Does Colima Die?

Three real causes, none of them sleep:

**1. macOS forces a restart (OS update, kernel panic)**
This is the most common cause. macOS will restart overnight for security updates. When it comes back up, Colima's launchd plist (`~/Library/LaunchAgents/colima.plist`) has `RunAtLoad: true` but **no `KeepAlive`**. This means:
- Colima starts correctly on boot ✅
- But `colima start` is a one-shot command — it starts the VM and exits
- If the VM crashes *after* starting, launchd does nothing
- This is the gap

**2. Virtualization.framework crash**
Your Colima is configured with `--vm-type vz` (Apple's Virtualization.framework). VZ is generally solid on M4, but can crash on macOS point releases or when memory pressure is extreme. 8GB allocated to Colima out of 36GB total is conservative — that's fine.

**3. T7 or T9 SSD unmounted**
If the T7 SSD (Ollama models) unmounts unexpectedly, Ollama will fail. If T9 (PostgreSQL data) unmounts, the database crashes. These aren't Colima itself, but they present identically — "things stopped working."

### The Fix

The Telegram bot will check Colima status every 5 minutes. If it's down, it attempts a restart and alerts you. This is more reliable than trying to make the launchd plist self-healing (because `colima start` is not a daemon process — it can't use KeepAlive cleanly).

---

## Part 2 — Wake on LAN

### Good News: It's Already Working

The `pmset` output shows:
```
womp    1                  ← Wake on Magic Packet: ENABLED
MAGICWAKE  en1             ← kernel-level WOL active on your ethernet port
```

Wake on LAN (WOL) works like this: a device on your network sends a "magic packet" (a special UDP broadcast containing your Mac's MAC address) to your Mac Studio. The network card sees it and powers the machine on — even from a full off state.

### What This Means for You

**On your home network:** Any device can wake the Mac Studio. Even a phone with a WOL app. This works today.

**From outside your home (remotely):** Your router needs to forward the WOL magic packet from the internet to the Mac Studio. Most home routers support "Remote Wake on LAN" or "WOL via port forwarding" — you'd forward UDP port 9 to the Mac Studio's IP (192.168.1.10).

**The honest caveat:** If the Mac Studio is completely powered off and *nothing else is running at home*, the Telegram bot can't wake it — because the bot is ON the Mac Studio. The bot can only send messages when it's running.

**Practical recommendation:** The real solution is preventing the Mac from fully shutting down in the first place, which `sleep 0` + `autorestart 1` already does. The only scenario where you need WOL is a power outage where autorestart fails — rare, and for that, configuring your router's WOL forwarding is a one-time 5-minute task.

---

## Part 3 — The Telegram Bot

### How It Works (for someone new to Telegram bots)

1. You go to @BotFather on Telegram and create a bot — takes 2 minutes. You get a **token** (a secret key).
2. You write a Python script that uses that token to connect to Telegram's servers.
3. When you send the bot a message from any device, Telegram delivers it to your Mac Studio.
4. The bot runs as a launchd daemon — starts on boot, always listening.
5. **Authorization:** The bot stores your personal Telegram **chat ID** (a number). It silently ignores messages from any other chat ID. Nobody else can talk to it.

You can send commands like `/status` and get a reply, or just type `казвам` and get back the lemma and translation.

---

### Architecture

```
Your iPhone / MacBook / iPad
        │
        │  Telegram (HTTPS)
        ▼
  Telegram Servers
        │
        │  Long-polling (bot asks "any new messages?")
        ▼
  Mac Studio
  ┌─────────────────────────────────────────────────────┐
  │  telegram_bot.py  (launchd daemon, outside Docker)  │
  │                                                      │
  │  ┌──────────────┐    ┌──────────────────────────┐   │
  │  │  Commands    │    │  Background monitors     │   │
  │  │  /status     │    │  Colima health (5 min)   │   │
  │  │  /restart    │    │  Container health (5 min)│   │
  │  │  /word X     │    │  Disk space (hourly)     │   │
  │  │  /stats      │    │  Vault seal (5 min)      │   │
  │  │  /queue      │    │  SSL cert expiry (daily) │   │
  │  └──────┬───────┘    └──────────────────────────┘   │
  │         │                                            │
  │         ▼                                            │
  │  Shell commands + HTTP calls to localhost:8080       │
  └─────────────────────────────────────────────────────┘
        │
        ├── colima start/stop
        ├── docker-compose up/down
        ├── vault status
        └── GET /api/admin/stats  (Spring Boot)
```

The bot calls your existing Spring Boot admin API for application-level information (word counts, sentence queue, failed lemmas). It calls shell commands directly for infrastructure (Colima, Docker, disk).

---

### Bulgarian Learning Features (Your #1 Priority)

#### Daily Morning Message — 8:00 AM
Sent automatically every morning:

```
📚 Добро утро, Kevin!

Your vocabulary: 247 words
SRS cards due today: 14
New words (never studied): 3

Today's word:
🇧🇬 намирам се
🇬🇧 to be located / to find oneself
📝 Той се намира в центъра на града.
    (He is located in the city centre.)

Type /study for full stats or /word [word] to look something up.
```

#### Pre-Lesson Alert — 30 Minutes Before Elena
Sent automatically before scheduled lessons (you tell the bot your lesson days/times):

```
🎓 Lesson with Elena in 30 minutes!

Since your last lesson (6 days ago):
  ✅ 8 new words added
  ✅ 8 words fully processed
  ✅ 42 example sentences generated

Words added recently:
  намирам се, тръгвам, наближавам, пристигам...

SRS cards due: 14 (good time to review before class)
```

#### Quick Word Lookup
Type any Bulgarian word (or English) directly into the chat:

```
You:  казвам се

Bot:  🔍 казвам се
      📖 Lemma: казвам се
      🏷  Verb (reflexive) · Beginner
      🇬🇧 to be called / my name is

      Forms: казвам се · казваш се · казва се
             казваме се · казвате се · казват се

      Example: Казвам се Кевин. Приятно ми е.
               (My name is Kevin. Nice to meet you.)
```

#### SRS Nudge (if cards go unreviewed)
```
⏰ You have 19 SRS cards due — 5 more than yesterday.
   Open the app to study: https://hagelbg.dyndns-ip.com
```

#### Word Ready Notification
When you add words and the LLM pipeline finishes:
```
✅ Processing complete!
   4 words ready: намирам се, тръгвам, наближавам, пристигам
   52 inflections and 16 example sentences generated.
```

#### Weekly Learning Summary (Sunday evening)
```
📊 Week of 17–23 February

Words added:    12
Words studied:  8
Reviews done:   47  (84% correct)
Study streak:   4 days

Total vocabulary: 247 words
Sentences generated: 312 / 247 words (all done ✅)

Keep it up — Златина will be impressed 😊
```

---

### Infrastructure Monitoring

#### Proactive Alerts (no action needed from you)

| Trigger | Message |
|---------|---------|
| Colima stops | 🔴 `Colima VM is down. Attempting restart...` |
| Colima restart succeeded | 🟢 `Colima restarted. All containers back up.` |
| Docker container unhealthy | 🔴 `Container [backend] is unhealthy. Run /restart backend` |
| Vault sealed | 🔴 `Vault is sealed. Secrets unavailable. Run /vault-unseal` |
| T7 SSD > 80% full | 🟡 `T7-NorthStar: 1.4TB / 1.8TB used (80%). Check Ollama models.` |
| T9 SSD > 80% full | 🟡 `T9-NorthStar: 2.9TB / 3.6TB used (80%). Check database.` |
| SSL cert expiry < 30 days | 🟡 `SSL cert expires in 28 days. Run certbot renew.` |
| SSL cert expiry < 7 days | 🔴 `SSL cert expires in 6 days! Renew immediately.` |
| nginx not responding | 🔴 `nginx is not responding on port 443.` |
| LLM circuit breaker open | 🟡 `Ollama circuit breaker opened. LLM calls are failing.` |
| Word stuck in PROCESSING >15min | 🟡 `Word "намирам се" has been PROCESSING for 20 min. Run /retry-failed` |

#### Commands You Can Send

```
/status           Full system health at a glance
/containers       Docker container list with status
/restart colima   Restart Colima VM (+ waits, confirms)
/restart backend  Restart Spring Boot container
/restart all      Restart entire Docker Compose stack
/logs backend     Last 30 lines of backend logs
/disk             Disk usage for T7, T9, and internal SSD
/ollama           Which model is loaded, GPU usage
/vault            Vault seal status
/cache-clear      Clear Redis/Valkey LLM response cache
/retry-failed     Trigger reprocessing of failed vocabulary words
/queue            Sentence generation progress
/word [text]      Look up a Bulgarian word
/stats            Your vocabulary and SRS statistics
/help             Full command reference
```

#### What a `/status` Reply Looks Like

```
🖥  Mac Studio Status — 09:47

Colima:    ✅ Running (VZ, 4 CPU, 8GB)
Backend:   ✅ Healthy
Valkey:    ✅ Healthy
Postgres:  ✅ Healthy
Vault:     ✅ Unsealed
nginx:     ✅ 443 responding
Ollama:    ✅ bggpt-vocab loaded (GPU)

Disk:
  Internal:  99GB / 460GB (23%)
  T7:         56GB / 1.8TB (4%)  ← Ollama models
  T9:        196MB / 3.6TB (<1%) ← PostgreSQL data

App:
  Words: 247 total · 3 failed · 0 stuck
  Sentences: 312/247 done (100%) ✅
  SRS due today: 14
```

---

## Part 4 — Other Mac Studio Uses for Telegram

Things not related to the vocabulary app that a bot on the Mac Studio could do:

**Home automation-adjacent:**
- Alert when an external USB drive mounts or unmounts (T7/T9 cable issues)
- Alert when Time Machine backup completes or fails
- Alert if the public IP address changes (your DynDNS may lag)
- Notify if an unexpected SSH login occurs

**Mac Studio health:**
- CPU temperature alerts when Ollama is running a big batch (M4 Max runs warm under load)
- Memory pressure alerts if the system is near its 36GB limit
- Alert if Spotlight or another process is hammering the CPU for extended time

**Remote control:**
- `/shell [command]` — run an allowlisted shell command remotely (powerful; only safe because only your chat ID is authorized)
- `/open [app]` — open an application on the Mac Studio
- `/say [text]` — have the Mac Studio speak a Bulgarian phrase via text-to-speech (useful for pronunciation practice when away from the machine)

**Future vocabulary features:**
- `/quiz` — the bot sends you a Bulgarian word, you reply with the translation, it scores you
- `/lesson-notes` — bot retrieves and summarizes your latest Obsidian lesson note
- `/homework` — shows your outstanding homework sentences from the latest lesson
- Notify Huw that new shared words have been added (if you share vocabulary)

---

## Part 5 — Implementation Plan

### Tech Stack

| Component | Choice | Reason |
|-----------|--------|--------|
| Language | Python 3.14 (already installed) | Standard for Telegram bots; no new runtime |
| Library | `python-telegram-bot` v21 | Most mature, async, well-documented |
| Scheduler | APScheduler (inside the bot) | Clean cron-like scheduling without systemd |
| Service | launchd plist | Consistent with Vault, Ollama, everything else on this Mac |
| Config | `.env` file (bot token, chat ID) | Same pattern as the rest of the project |
| HTTP client | `httpx` (async) | Calls Spring Boot admin API |

### Files to Create

```
telegram-bot/
├── bot.py               Main bot — commands, alerts, scheduler
├── checks.py            Infrastructure health check functions
├── vocab.py             Spring Boot API client (word lookup, stats, queue)
├── scheduler.py         Daily word, pre-lesson brief, weekly summary
├── config.py            Load bot token, chat ID, lesson schedule from env
├── requirements.txt
├── .env.example         (committed — shows what vars are needed)
└── README.md

~/Library/LaunchAgents/
└── com.bgvocab.telegram-bot.plist   launchd daemon
```

### Phase 1 Scope (what to build first)

Start with the features that will make you say "I can't believe I didn't have this before":

1. `/status` command — instant Mac Studio health check
2. Colima + container monitoring with auto-restart attempt
3. Word processing completion notification ("your words are ready")
4. Daily 8am vocabulary message with word of the day
5. `/word [term]` lookup

**Phase 2** (after Phase 1 is running and stable):
- Pre-lesson briefing
- SRS nudge
- `/quiz` mode
- Full infrastructure alert suite (SSL, disk, Vault)

---

## Open Questions Before Building

1. **Lesson schedule:** When are your Elena lessons? (day of week, time) — needed for pre-lesson alert
2. **Word of the day source:** Should the bot pick a random word from your existing vocabulary, or always pick one that's "due for review"?
3. **Bot token:** You'll need to create the bot via @BotFather on Telegram first. Takes 2 minutes.
4. **Alert sensitivity:** Should infrastructure alerts wake you at 2am, or only during certain hours?

---

*This document will be updated as the design is approved and implementation begins.*
