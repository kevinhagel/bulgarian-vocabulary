#!/usr/bin/env python3
"""
Bulgarian Vocabulary — Telegram Bot
Runs as a launchd service on Mac Studio.
"""
import asyncio
import logging
import subprocess
import sys
from datetime import datetime, time as dtime, timezone
from pathlib import Path

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from telegram import Update
from telegram.constants import ParseMode
from telegram.ext import (
    Application,
    CommandHandler,
    ContextTypes,
    MessageHandler,
    filters,
)

import config
import checks
import vocab
import scheduler as sched

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    stream=sys.stdout,
)
log = logging.getLogger("bot")


# ── Authorization guard ───────────────────────────────────────────────────────

def authorized(update: Update) -> bool:
    return update.effective_chat.id == config.CHAT_ID


async def deny(update: Update) -> None:
    log.warning("Unauthorized access from chat_id=%s", update.effective_chat.id)
    # Silently ignore — don't reveal the bot exists to strangers


# ── Helpers ───────────────────────────────────────────────────────────────────

def _esc(text: str) -> str:
    """Escape MarkdownV2 special chars."""
    specials = r"\_*[]()~`>#+-=|{}.!"
    return "".join(f"\\{c}" if c in specials else c for c in str(text))


def _tick(ok: bool) -> str:
    return "✅" if ok else "❌"


async def _send(update: Update, text: str, md: bool = False) -> None:
    await update.message.reply_text(
        text,
        parse_mode=ParseMode.MARKDOWN_V2 if md else None,
    )


# ── /start ────────────────────────────────────────────────────────────────────

async def cmd_start(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    await _send(update,
        "👋 Здравейте! I'm your Bulgarian Vocabulary assistant.\n\n"
        "Type /help for all commands."
    )


# ── /help ─────────────────────────────────────────────────────────────────────

async def cmd_help(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    text = (
        "*Learning*\n"
        "/word \\[text\\] — look up a Bulgarian or English word\n"
        "/stats — vocabulary and SRS statistics\n"
        "/queue — sentence generation progress\n"
        "/lesson\\_today — send pre\\-lesson brief now\n"
        "/retry\\_failed — reprocess all failed words\n"
        "\n"
        "*Infrastructure*\n"
        "/status — full system health\n"
        "/containers — Docker container list\n"
        "/disk — disk usage for all drives\n"
        "/ollama — model loaded and GPU status\n"
        "/vault — Vault seal status\n"
        "/logs \\[name\\] — last 30 lines of container logs\n"
        "/cache\\_clear — clear Valkey LLM response cache\n"
        "/restart colima|backend|all — restart a service\n"
    )
    await _send(update, text, md=True)


# ── /status ───────────────────────────────────────────────────────────────────

async def cmd_status(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return

    now = datetime.now().strftime("%H:%M")

    colima = await asyncio.get_event_loop().run_in_executor(None, checks.colima_status)
    containers = await asyncio.get_event_loop().run_in_executor(None, checks.container_states)
    vault = await asyncio.get_event_loop().run_in_executor(None, checks.vault_status)
    nginx_ok = await asyncio.get_event_loop().run_in_executor(None, checks.nginx_responding)
    ollama = await asyncio.get_event_loop().run_in_executor(None, checks.ollama_status)
    disks = await asyncio.get_event_loop().run_in_executor(None, checks.disk_usage)
    stats = await vocab.get_admin_stats()
    progress = await vocab.get_progress()

    # Container lookup helpers
    def cstatus(name_fragment: str) -> str:
        for c in containers:
            if name_fragment in c["name"]:
                return _tick(c["healthy"])
        return "❓"

    vault_icon = _tick(not vault["sealed"]) if vault["ok"] else "❓"

    lines = [
        f"🖥  *Mac Studio Status — {_esc(now)}*",
        "",
        f"Colima:   {_tick(colima['running'])} {'Running' if colima['running'] else 'DOWN'}",
        f"Backend:  {cstatus('backend')}",
        f"Valkey:   {cstatus('valkey')}",
        f"Postgres: {cstatus('db')}",
        f"Vault:    {vault_icon} {'Unsealed' if vault['ok'] and not vault['sealed'] else 'Sealed/unknown'}",
        f"nginx:    {_tick(nginx_ok)} {'443 OK' if nginx_ok else 'not responding'}",
        f"Ollama:   {_tick(ollama['running'])} {_esc(ollama.get('model', ''))} {_esc(ollama.get('processor', ''))}",
        "",
        "*Disk:*",
    ]

    for d in disks:
        icon = "🔴" if d["pct"] >= 90 else ("🟡" if d["pct"] >= 80 else "⚪")
        lines.append(f"  {icon} {_esc(d['label'])}: {d['used_gb']:.0f}GB / {d['total_gb']:.0f}GB \\({d['pct']}%\\)")

    if stats:
        ls = stats["lemmas"]
        ss = stats["sentences"]
        total_s = ss["done"] + ss["none"] + ss["queued"] + ss["generating"] + ss["failed"]
        sentence_pct = f"{ss['done']}/{total_s}" if total_s else "0"
        due = progress.get("cardsDueToday", "?") if progress else "?"
        lines += [
            "",
            "*App:*",
            f"  Words: {ls['total']} total · {ls['failed']} failed · {ls['processing']} stuck",
            f"  Sentences: {sentence_pct} done",
            f"  SRS due today: {due}",
        ]

    await _send(update, "\n".join(lines), md=True)


# ── /containers ───────────────────────────────────────────────────────────────

async def cmd_containers(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    containers = await asyncio.get_event_loop().run_in_executor(None, checks.container_states)
    if not containers:
        await _send(update, "No containers running (or Docker/Colima is down).")
        return
    lines = ["*Docker Containers:*\n"]
    for c in containers:
        icon = "✅" if c["healthy"] else "🔴"
        lines.append(f"{icon} {_esc(c['name'])}\n   `{_esc(c['status'])}`")
    await _send(update, "\n".join(lines), md=True)


# ── /disk ─────────────────────────────────────────────────────────────────────

async def cmd_disk(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    disks = await asyncio.get_event_loop().run_in_executor(None, checks.disk_usage)
    if not disks:
        await _send(update, "Could not read disk usage.")
        return
    lines = ["*Disk Usage:*\n"]
    for d in disks:
        icon = "🔴" if d["pct"] >= 90 else ("🟡" if d["pct"] >= 80 else "🟢")
        lines.append(f"{icon} *{_esc(d['label'])}*")
        lines.append(f"   {d['used_gb']:.1f} GB / {d['total_gb']:.1f} GB \\({d['pct']}% used\\)")
    await _send(update, "\n".join(lines), md=True)


# ── /ollama ───────────────────────────────────────────────────────────────────

async def cmd_ollama(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    o = await asyncio.get_event_loop().run_in_executor(None, checks.ollama_status)
    if not o["running"]:
        await _send(update, "❌ Ollama is not running.")
        return
    model = o.get("model", "none loaded")
    proc = o.get("processor", "?")
    await _send(update,
        f"*Ollama*\nModel: `{_esc(model)}`\nProcessor: `{_esc(proc)}`",
        md=True
    )


# ── /vault ────────────────────────────────────────────────────────────────────

async def cmd_vault(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    v = await asyncio.get_event_loop().run_in_executor(None, checks.vault_status)
    if not v["ok"]:
        await _send(update, "❓ Could not reach Vault at http://127.0.0.1:8200")
        return
    state = "🔒 *Sealed* — secrets unavailable" if v["sealed"] else "✅ *Unsealed* — all good"
    await _send(update, state, md=True)


# ── /logs ─────────────────────────────────────────────────────────────────────

async def cmd_logs(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    args = ctx.args
    name = args[0] if args else "backend"
    logs = await asyncio.get_event_loop().run_in_executor(None, checks.container_logs, name, 30)
    if not logs:
        await _send(update, f"No logs found for container '{name}'. Check the name with /containers.")
        return
    # Telegram message limit is 4096 chars
    if len(logs) > 3800:
        logs = "...(truncated)\n" + logs[-3800:]
    await _send(update, f"```\n{logs}\n```", md=True)


# ── /restart ─────────────────────────────────────────────────────────────────

async def cmd_restart(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    args = ctx.args
    target = args[0].lower() if args else ""

    if target == "colima":
        await _send(update, "🔄 Restarting Colima... (this takes ~30s)")
        # Stop then start
        subprocess.run(["colima", "stop"], capture_output=True, timeout=60)
        ok = await asyncio.get_event_loop().run_in_executor(None, checks.colima_start)
        if ok:
            await _send(update, "✅ Colima restarted. Containers may need a moment to recover.")
        else:
            await _send(update, "❌ Colima restart failed. Check logs on the machine.")

    elif target in ("backend", "postgres", "valkey", "pgadmin", "db"):
        # Map friendly names to actual container names
        name_map = {"postgres": "bulgarian-vocab-db", "db": "bulgarian-vocab-db",
                    "valkey": "bulgarian-vocab-valkey", "backend": "bulgarian-vocab-backend",
                    "pgadmin": "bulgarian-vocab-pgadmin"}
        container_name = name_map.get(target, target)
        await _send(update, f"🔄 Restarting container '{container_name}'...")
        ok = await asyncio.get_event_loop().run_in_executor(None, checks.restart_container, container_name)
        await _send(update, f"{'✅' if ok else '❌'} Container '{container_name}' restart {'succeeded' if ok else 'failed'}.")

    elif target == "all":
        await _send(update, "🔄 Running docker compose up -d...")
        ok = await asyncio.get_event_loop().run_in_executor(None, checks.compose_up)
        await _send(update, f"{'✅ All containers up.' if ok else '❌ docker compose up failed.'}")

    else:
        await _send(update,
            "Usage: /restart colima|backend|postgres|valkey|all"
        )


# ── /word ─────────────────────────────────────────────────────────────────────

async def cmd_word(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    query = " ".join(ctx.args).strip() if ctx.args else ""
    if not query:
        await _send(update, "Usage: /word [Bulgarian or English word]")
        return
    results = await vocab.search_word(query)
    text = vocab.format_word_lookup(results, query)
    await _send(update, text)


# ── /stats ────────────────────────────────────────────────────────────────────

async def cmd_stats(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    stats = await vocab.get_admin_stats()
    progress = await vocab.get_progress()

    if not stats:
        await _send(update, "⚠️ Could not reach backend.")
        return

    ls = stats["lemmas"]
    ss = stats["sentences"]
    total_sentences = ss["done"] + ss["none"] + ss["queued"] + ss["generating"] + ss["failed"]
    due = progress.get("cardsDueToday", "?") if progress else "?"
    new = progress.get("newCards", "?") if progress else "?"
    retention = progress.get("retentionRate", "?") if progress else "?"
    sessions = progress.get("totalSessions", "?") if progress else "?"
    reviewed = progress.get("totalCardsReviewed", "?") if progress else "?"

    lines = [
        "*📊 Vocabulary Stats*\n",
        f"Total words: *{ls['total']}*",
        f"  Completed: {ls['completed']}",
        f"  Failed: {ls['failed']}",
        f"  In queue/processing: {ls['queued'] + ls['processing']}",
        f"  Reviewed: {ls['reviewed']}",
        "",
        f"Sentences: *{ss['done']}/{total_sentences}* done",
        "",
        "*SRS:*",
        f"  Due today: *{due}*",
        f"  Never studied: {new}",
        f"  Total reviews: {reviewed}",
        f"  Retention: {retention}%",
        f"  Sessions: {sessions}",
    ]

    if stats.get("failedLemmas"):
        lines.append(f"\n⚠️ Failed words: {', '.join(f['text'] for f in stats['failedLemmas'][:5])}")
    if stats.get("stuckLemmas"):
        lines.append(f"⏳ Stuck words: {', '.join(f['text'] for f in stats['stuckLemmas'][:5])}")

    await _send(update, "\n".join(lines), md=True)


# ── /queue ────────────────────────────────────────────────────────────────────

async def cmd_queue(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    stats = await vocab.get_admin_stats()
    if not stats:
        await _send(update, "⚠️ Could not reach backend.")
        return
    ss = stats["sentences"]
    total = ss["done"] + ss["none"] + ss["queued"] + ss["generating"] + ss["failed"]
    pct = int(ss["done"] / total * 100) if total else 0
    bar_filled = int(pct / 10)
    bar = "█" * bar_filled + "░" * (10 - bar_filled)

    lines = [
        "*📨 Sentence Generation Queue*\n",
        f"`[{bar}]` {pct}%\n",
        f"Done:        {ss['done']}",
        f"Queued:      {ss['queued']}",
        f"Generating:  {ss['generating']}",
        f"No sentence: {ss['none']}",
        f"Failed:      {ss['failed']}",
    ]
    await _send(update, "\n".join(lines), md=True)


# ── /cache_clear ──────────────────────────────────────────────────────────────

async def cmd_cache_clear(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    ok = await vocab.clear_cache()
    await _send(update, "✅ LLM response cache cleared." if ok else "❌ Cache clear failed.")


# ── /retry_failed ─────────────────────────────────────────────────────────────

async def cmd_retry_failed(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    await _send(update, "🔄 Reprocessing failed/stuck words...")
    results = await vocab.retry_failed_words()
    if not results:
        await _send(update, "No failed or stuck words found. ✅")
    else:
        names = ", ".join(r["text"] for r in results[:10])
        await _send(update, f"✅ Queued {len(results)} word(s) for reprocessing:\n{names}")


# ── /lesson_today ─────────────────────────────────────────────────────────────

async def cmd_lesson_today(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    await sched.send_lesson_brief(ctx.bot, config.CHAT_ID)


# ── Free-text word lookup ─────────────────────────────────────────────────────

async def on_text(update: Update, ctx: ContextTypes.DEFAULT_TYPE) -> None:
    if not authorized(update):
        return
    query = update.message.text.strip()
    if not query or query.startswith("/"):
        return
    results = await vocab.search_word(query)
    text = vocab.format_word_lookup(results, query)
    await _send(update, text)


# ── Background monitors ───────────────────────────────────────────────────────

async def _monitor_colima(bot) -> None:
    """Check Colima every 5 min; alert and attempt restart if down."""
    if _is_quiet():
        return
    status = await asyncio.get_event_loop().run_in_executor(None, checks.colima_status)
    if not status["running"]:
        log.warning("Colima is DOWN — attempting restart")
        await bot.send_message(config.CHAT_ID, "🔴 Colima VM is down\\. Attempting restart\\.\\.\\.", parse_mode="MarkdownV2")
        ok = await asyncio.get_event_loop().run_in_executor(None, checks.colima_start)
        if ok:
            await bot.send_message(config.CHAT_ID, "🟢 Colima restarted\\. Containers should be up\\.", parse_mode="MarkdownV2")
        else:
            await bot.send_message(config.CHAT_ID, "❌ Colima restart *failed*\\. Manual intervention needed\\.", parse_mode="MarkdownV2")


async def _monitor_containers(bot) -> None:
    """Alert on unhealthy containers."""
    if _is_quiet():
        return
    containers = await asyncio.get_event_loop().run_in_executor(None, checks.container_states)
    for c in containers:
        if not c["healthy"] and "unhealthy" in c["status"].lower():
            name = _esc(c["name"])
            await bot.send_message(
                config.CHAT_ID,
                f"🔴 Container *{name}* is unhealthy\\.\nRun /restart {name} or check /logs {name}",
                parse_mode="MarkdownV2"
            )


async def _monitor_disk(bot) -> None:
    """Alert if any disk is over 80%."""
    if _is_quiet():
        return
    disks = await asyncio.get_event_loop().run_in_executor(None, checks.disk_usage)
    for d in disks:
        if d["pct"] >= 90:
            await bot.send_message(
                config.CHAT_ID,
                f"🔴 *{_esc(d['label'])}* is {d['pct']}% full\\! \\({d['used_gb']:.0f}GB / {d['total_gb']:.0f}GB\\)",
                parse_mode="MarkdownV2"
            )
        elif d["pct"] >= 80:
            await bot.send_message(
                config.CHAT_ID,
                f"🟡 *{_esc(d['label'])}*: {d['pct']}% full \\({d['used_gb']:.0f}GB / {d['total_gb']:.0f}GB\\)",
                parse_mode="MarkdownV2"
            )


async def _monitor_vault(bot) -> None:
    """Alert if Vault becomes sealed."""
    if _is_quiet():
        return
    v = await asyncio.get_event_loop().run_in_executor(None, checks.vault_status)
    if v["ok"] and v["sealed"]:
        await bot.send_message(
            config.CHAT_ID,
            "🔴 *Vault is sealed\\!* Secrets unavailable\\. The auto\\-unseal service should fix this within 30s\\.",
            parse_mode="MarkdownV2"
        )


async def _monitor_ssl(bot) -> None:
    """Alert if SSL cert expiring soon."""
    if _is_quiet():
        return
    days = await asyncio.get_event_loop().run_in_executor(None, checks.ssl_days_remaining)
    if days is None:
        return
    if days < 7:
        await bot.send_message(
            config.CHAT_ID,
            f"🔴 SSL cert expires in *{days} days\\!* Renew immediately\\.",
            parse_mode="MarkdownV2"
        )
    elif days < 30:
        await bot.send_message(
            config.CHAT_ID,
            f"🟡 SSL cert expires in *{days} days*\\. Run certbot renew soon\\.",
            parse_mode="MarkdownV2"
        )


def _is_quiet() -> bool:
    h = datetime.now().hour
    return config.QUIET_START <= h < config.QUIET_END


# ── Startup notification ───────────────────────────────────────────────────────

async def send_startup_message(bot) -> None:
    try:
        await bot.send_message(
            config.CHAT_ID,
            "🤖 Bulgarian Vocabulary bot started\\. Type /status or /help\\.",
            parse_mode="MarkdownV2"
        )
    except Exception as e:
        log.warning("Could not send startup message (send /start to the bot first): %s", e)


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    app = Application.builder().token(config.BOT_TOKEN).build()

    # Commands
    app.add_handler(CommandHandler("start", cmd_start))
    app.add_handler(CommandHandler("help", cmd_help))
    app.add_handler(CommandHandler("status", cmd_status))
    app.add_handler(CommandHandler("containers", cmd_containers))
    app.add_handler(CommandHandler("disk", cmd_disk))
    app.add_handler(CommandHandler("ollama", cmd_ollama))
    app.add_handler(CommandHandler("vault", cmd_vault))
    app.add_handler(CommandHandler("logs", cmd_logs))
    app.add_handler(CommandHandler("restart", cmd_restart))
    app.add_handler(CommandHandler("word", cmd_word))
    app.add_handler(CommandHandler("stats", cmd_stats))
    app.add_handler(CommandHandler("queue", cmd_queue))
    app.add_handler(CommandHandler("cache_clear", cmd_cache_clear))
    app.add_handler(CommandHandler("retry_failed", cmd_retry_failed))
    app.add_handler(CommandHandler("lesson_today", cmd_lesson_today))

    # Free-text lookup
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, on_text))

    # APScheduler for daily jobs
    scheduler = AsyncIOScheduler(timezone="Europe/Sofia")

    # 08:00 — word of the day
    wod_h, wod_m = map(int, config.WORD_OF_DAY_TIME.split(":"))
    scheduler.add_job(
        lambda: asyncio.ensure_future(sched.send_morning_message(app.bot, config.CHAT_ID)),
        "cron", hour=wod_h, minute=wod_m,
        id="morning_message"
    )

    # 18:00 — SRS nudge (if cards due >= 5)
    srs_h, srs_m = map(int, config.SRS_NUDGE_TIME.split(":"))
    scheduler.add_job(
        lambda: asyncio.ensure_future(sched.send_srs_nudge(app.bot, config.CHAT_ID)),
        "cron", hour=srs_h, minute=srs_m,
        id="srs_nudge"
    )

    # Sunday 20:00 — weekly summary
    ws_h, ws_m = map(int, config.WEEKLY_SUMMARY_TIME.split(":"))
    scheduler.add_job(
        lambda: asyncio.ensure_future(sched.send_weekly_summary(app.bot, config.CHAT_ID)),
        "cron", day_of_week=config.WEEKLY_SUMMARY_DAY, hour=ws_h, minute=ws_m,
        id="weekly_summary"
    )

    # Every 5 min — Colima + container health
    scheduler.add_job(
        lambda: asyncio.ensure_future(_monitor_colima(app.bot)),
        "interval", minutes=5, id="monitor_colima"
    )
    scheduler.add_job(
        lambda: asyncio.ensure_future(_monitor_containers(app.bot)),
        "interval", minutes=5, id="monitor_containers"
    )

    # Every 5 min — Vault seal check
    scheduler.add_job(
        lambda: asyncio.ensure_future(_monitor_vault(app.bot)),
        "interval", minutes=5, id="monitor_vault"
    )

    # Hourly — disk space
    scheduler.add_job(
        lambda: asyncio.ensure_future(_monitor_disk(app.bot)),
        "interval", hours=1, id="monitor_disk"
    )

    # Daily — SSL cert expiry
    scheduler.add_job(
        lambda: asyncio.ensure_future(_monitor_ssl(app.bot)),
        "cron", hour=9, minute=0, id="monitor_ssl"
    )

    async def post_init(application: Application) -> None:
        scheduler.start()
        await send_startup_message(application.bot)

    app.post_init = post_init

    log.info("Starting bot (polling)...")
    app.run_polling(allowed_updates=Update.ALL_TYPES)


if __name__ == "__main__":
    main()
