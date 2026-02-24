"""Spring Boot API client — vocabulary, stats, admin."""
import httpx
from config import BACKEND_URL, BOT_API_KEY

_HEADERS = {"X-Bot-Token": BOT_API_KEY}


async def get_admin_stats() -> dict | None:
    async with httpx.AsyncClient(timeout=10, headers=_HEADERS) as client:
        try:
            r = await client.get(f"{BACKEND_URL}/api/admin/stats")
            r.raise_for_status()
            return r.json()
        except Exception:
            return None


async def get_progress() -> dict | None:
    async with httpx.AsyncClient(timeout=10, headers=_HEADERS) as client:
        try:
            r = await client.get(f"{BACKEND_URL}/api/study/progress")
            r.raise_for_status()
            return r.json()
        except Exception:
            return None


async def search_word(query: str) -> list[dict]:
    async with httpx.AsyncClient(timeout=10, headers=_HEADERS) as client:
        try:
            r = await client.get(f"{BACKEND_URL}/api/vocabulary/search", params={"q": query})
            r.raise_for_status()
            return r.json()
        except Exception:
            return []


async def get_lemma(lemma_id: int) -> dict | None:
    async with httpx.AsyncClient(timeout=10, headers=_HEADERS) as client:
        try:
            r = await client.get(f"{BACKEND_URL}/api/vocabulary/{lemma_id}")
            r.raise_for_status()
            return r.json()
        except Exception:
            return None


async def clear_cache() -> bool:
    async with httpx.AsyncClient(timeout=10, headers=_HEADERS) as client:
        try:
            r = await client.post(f"{BACKEND_URL}/api/admin/cache/clear")
            return r.status_code == 204
        except Exception:
            return False


async def retry_failed_words() -> list[dict]:
    """Reprocess all failed lemmas. Returns list of reprocessed items."""
    stats = await get_admin_stats()
    if not stats:
        return []
    failed = stats.get("failedLemmas", []) + stats.get("stuckLemmas", [])
    results = []
    async with httpx.AsyncClient(timeout=10) as client:
        for lemma in failed:
            try:
                r = await client.post(f"{BACKEND_URL}/api/vocabulary/{lemma['id']}/reprocess")
                if r.status_code == 200:
                    results.append(lemma)
            except Exception:
                pass
    return results


def format_word_lookup(results: list[dict], query: str) -> str:
    if not results:
        return f'🔍 No results found for "{query}"'

    entry = results[0]
    lines = [f"🔍 *{entry.get('text', query)}*"]

    pos = entry.get("partOfSpeech", "")
    difficulty = entry.get("difficultyLevel", "")
    if pos or difficulty:
        tag = " · ".join(filter(None, [pos.replace("_", " ").title() if pos else "", difficulty.title() if difficulty else ""]))
        lines.append(f"🏷  {tag}")

    translation = entry.get("translation", "")
    if translation:
        lines.append(f"🇬🇧 {translation}")

    inflections = entry.get("inflections", [])
    if inflections:
        forms = [i.get("form", "") for i in inflections[:6] if i.get("form")]
        if forms:
            lines.append(f"📐 Forms: {' · '.join(forms)}")

    sentences = entry.get("sentences", [])
    if sentences:
        s = sentences[0]
        bg = s.get("bulgarianText", "")
        en = s.get("englishTranslation", "")
        if bg:
            lines.append(f"\n📝 _{bg}_")
        if en:
            lines.append(f"    ({en})")

    if len(results) > 1:
        lines.append(f"\n_{len(results) - 1} more result(s) — try being more specific_")

    return "\n".join(lines)
