"""Scheduled jobs: word of the day, SRS nudge, lesson brief, weekly summary."""
import logging
import random
from datetime import datetime, date

import httpx

import config
from config import BOT_API_KEY
from vocab import get_admin_stats, get_progress, search_word, format_word_lookup

_HEADERS = {"X-Bot-Token": BOT_API_KEY}

log = logging.getLogger(__name__)


def _is_quiet() -> bool:
    h = datetime.now().hour
    return config.QUIET_START <= h < config.QUIET_END


async def _pick_word_of_day() -> dict | None:
    """Pick a random word from vocabulary for the daily message."""
    async with httpx.AsyncClient(timeout=10, headers=_HEADERS) as client:
        try:
            # Get a page of vocabulary — pick a random entry
            r = await client.get(
                f"{config.BACKEND_URL}/api/vocabulary",
                params={"source": "USER_ENTERED", "size": 50, "page": 0}
            )
            r.raise_for_status()
            data = r.json()
            content = data.get("content", [])
            if not content:
                return None
            entry = random.choice(content)
            # Fetch full detail for inflections + sentences
            r2 = await client.get(f"{config.BACKEND_URL}/api/vocabulary/{entry['id']}")
            r2.raise_for_status()
            return r2.json()
        except Exception:
            return None


async def send_morning_message(bot, chat_id: int) -> None:
    if _is_quiet():
        return
    stats = await get_admin_stats()
    progress = await get_progress()
    word = await _pick_word_of_day()

    total = stats["lemmas"]["total"] if stats else "?"
    due = progress["cardsDueToday"] if progress else "?"
    new_cards = progress["newCards"] if progress else "?"

    lines = [
        "📚 *Добро утро, Kevin\\!*",
        "",
        f"Your vocabulary: *{total}* words",
        f"SRS cards due today: *{due}*",
        f"New words \\(never studied\\): *{new_cards}*",
    ]

    if word:
        lines += [
            "",
            f"*Today's word:*",
            f"🇧🇬 *{_esc(word.get('text', ''))}*",
            f"🇬🇧 {_esc(word.get('translation', ''))}",
        ]
        sentences = word.get("sentences", [])
        if sentences:
            s = sentences[0]
            bg = s.get("bulgarianText", "")
            en = s.get("englishTranslation", "")
            if bg:
                lines.append(f"📝 _{_esc(bg)}_")
            if en:
                lines.append(f"    \\({_esc(en)}\\)")

    lines += ["", "Type /stats for full stats or /word \\[word\\] to look something up\\."]

    await bot.send_message(chat_id, "\n".join(lines), parse_mode="MarkdownV2")
    log.info("Sent morning message")


async def send_srs_nudge(bot, chat_id: int) -> None:
    if _is_quiet():
        return
    progress = await get_progress()
    if not progress:
        return
    due = progress.get("cardsDueToday", 0)
    if due < 5:
        return

    msg = (
        f"⏰ You have *{due}* SRS cards due\\.\n"
        f"Open the app to study: https://hagelbg\\.dyndns\\-ip\\.com"
    )
    await bot.send_message(chat_id, msg, parse_mode="MarkdownV2")
    log.info("Sent SRS nudge (%d cards due)", due)


async def send_weekly_summary(bot, chat_id: int) -> None:
    if _is_quiet():
        return
    stats = await get_admin_stats()
    progress = await get_progress()
    if not stats or not progress:
        return

    total = stats["lemmas"]["total"]
    sentences_done = stats["sentences"]["done"]
    total_reviewed = progress.get("totalCardsReviewed", 0)
    retention = progress.get("retentionRate", 0)
    sessions = progress.get("totalSessions", 0)

    today = date.today()
    week_start = today.strftime("%-d")
    week_end = today.strftime("%-d %B")

    all_done = sentences_done >= total
    sentence_status = "✅" if all_done else f"{sentences_done}/{total}"

    msg = (
        f"📊 *Weekly Summary*\n"
        f"\n"
        f"Total vocabulary: *{total}* words\n"
        f"Sentences generated: *{sentence_status}*\n"
        f"Reviews done: *{total_reviewed}* \\({retention}% correct\\)\n"
        f"Study sessions: *{sessions}*\n"
        f"\n"
        f"Keep it up — Златина will be impressed 😊"
    )
    await bot.send_message(chat_id, msg, parse_mode="MarkdownV2")
    log.info("Sent weekly summary")


async def send_lesson_brief(bot, chat_id: int) -> None:
    """Pre-lesson briefing — called manually via /lesson_today."""
    stats = await get_admin_stats()
    progress = await get_progress()
    if not stats or not progress:
        await bot.send_message(chat_id, "⚠️ Could not fetch stats from backend.")
        return

    total = stats["lemmas"]["total"]
    completed = stats["lemmas"]["completed"]
    sentences_done = stats["sentences"]["done"]
    due = progress.get("cardsDueToday", 0)

    msg = (
        f"🎓 *Lesson Brief*\n"
        f"\n"
        f"Vocabulary: *{total}* total · *{completed}* fully processed\n"
        f"Example sentences: *{sentences_done}*\n"
        f"SRS cards due now: *{due}*\n"
        f"\n"
        f"_Good luck with Elena\\!_"
    )
    await bot.send_message(chat_id, msg, parse_mode="MarkdownV2")


def _esc(text: str) -> str:
    """Escape MarkdownV2 special chars."""
    specials = r"\_*[]()~`>#+-=|{}.!"
    return "".join(f"\\{c}" if c in specials else c for c in str(text))
