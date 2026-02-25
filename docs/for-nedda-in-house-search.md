# In-House Search Stack — SearXNG + Perplexica + Ollama

*Kevin Hagel*

---

## Architecture

```
Client → Perplexica (port 3001)
              ↓
         SearXNG (port 8080)  ←  Valkey session cache
              ↓
    Google/Bing/DDG/etc (anonymised, no identity leaked)
              ↓
         Ollama (native, port 11434) — local LLM synthesis
              ↓
    Cited answer back to client
```

Three Docker containers + Ollama running natively. Everything stays on the box.

---

## SearXNG

Metasearch proxy. Fans queries out to configured engines, deduplicates, ranks. Engines are configurable per category (general, news, science, code, etc.). Has a JSON API — useful if you want to integrate search into your own applications.

The only dependency is a Redis-compatible cache for sessions — Valkey works fine.

One thing worth knowing: SearXNG's default `settings.yml` has most engines disabled. You configure which ones you want. The [engine list](https://docs.searxng.org/admin/engines/configured_engines.html) is extensive.

```yaml
# docker-compose snippet
searxng:
  image: searxng/searxng:latest
  ports:
    - "8080:8080"
  volumes:
    - ./searxng:/etc/searxng:rw   # put your settings.yml here
  environment:
    - SEARXNG_BASE_URL=http://your-host:8080/
    - SEARXNG_ENABLE_SEARCH_API=true
  depends_on:
    valkey:
      condition: service_healthy
```

---

## Perplexica

Open-source Perplexity. Takes SearXNG results, feeds them to a local LLM, returns a synthesised answer with citations. The UI is clean and usable.

Connects to Ollama (or LM Studio if you prefer an OpenAI-compatible API). On Apple Silicon, `llama3.1:8b` or `qwen2.5:14b` are good starting points — reasonable quality, fast enough for interactive use.

```yaml
perplexica:
  image: itzcrazykns1337/perplexica:latest
  ports:
    - "3001:3000"
  volumes:
    - ./perplexica/config.toml:/home/perplexica/config.toml:ro
  environment:
    - SEARXNG_API_URL=http://searxng:8080
    - NEXT_PUBLIC_API_URL=http://your-host:3001/api
    - NEXT_PUBLIC_WS_URL=ws://your-host:3001
  depends_on:
    searxng:
      condition: service_healthy
```

The `config.toml` is where you point it at your Ollama instance and set the default model.

One note from running this in production: Ollama's parallelism can cause issues when Perplexica fires multiple concurrent requests during a single search. LM Studio (OpenAI-compatible on port 1234) handled this more reliably in our setup. Worth keeping in mind if you see timeouts.

---

## Ollama

Runs natively — not in Docker. `brew install ollama` on Mac, or the Linux installer.

```bash
ollama pull llama3.1:8b      # general purpose
ollama pull qwen2.5:14b      # better reasoning, needs more RAM
ollama serve                  # starts on :11434
```

On Apple Silicon the models run mostly on the GPU via Metal. M-series chips handle 14B models comfortably with 32GB unified memory.

---

## PGroonga — Full-Text Search Inside PostgreSQL

Separate use case but related topic: if you need full-text search over your *own* data rather than the web, PGroonga is worth knowing about.

It's a PostgreSQL extension built on [Groonga](https://groonga.org). The key differentiator is proper handling of non-Latin scripts — Cyrillic, Arabic, CJK — without the stemmer configuration hell you'd have with `tsvector`/`tsquery` or Elasticsearch.

```sql
CREATE EXTENSION pgroonga;
CREATE INDEX ON documents USING pgroonga (content);

-- Works correctly for Cyrillic, no tokeniser config needed
SELECT title FROM documents WHERE content &@~ 'договор наем';
```

We use it for Bulgarian vocabulary search. No Elasticsearch cluster, no index sync, just a standard PostgreSQL extension. The query operator `&@~` supports full query syntax (AND, OR, NOT, phrase, prefix).

- Docs: https://pgroonga.github.io
- Install: https://pgroonga.github.io/install/
- The interesting part — how it handles scripts: https://pgroonga.github.io/reference/operators/query-v2.html

---

## What You Need

- Docker + Docker Compose
- Ollama (native install)
- ~16GB RAM minimum, 32GB comfortable for 14B models
- Apple Silicon or a decent Linux box — GPU acceleration via Metal / CUDA / ROCm

The full compose file (SearXNG + Perplexica + Valkey + Ollama pointer) is about 80 lines. Happy to share it.
