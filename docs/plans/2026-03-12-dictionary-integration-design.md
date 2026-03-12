# Dictionary Integration Design

**Date**: 2026-03-12
**Status**: Approved

## Problem

BgGPT-generated inflections are unreliable. Example: перо (feather) had pl.indef as "перата" instead of correct "пера". The LLM is the sole source of truth, and it makes mistakes that undermine trust in the study material.

## Solution

Integrate the Kaikki/Wiktionary Bulgarian dictionary (52,304 words, CC BY-SA 3.0) as the primary source of truth for inflections. BgGPT becomes a fallback for words not in the dictionary.

## Data Source

- **File**: `data/kaikki-bulgarian.jsonl` (117MB, gitignored)
- **Coverage**: 94% of existing 143 lemmas (135/143 matched)
- **Content per entry**: inflections with stress marks, multiple senses, romanization, etymology, IPA, derived words
- **License**: CC BY-SA 3.0

## Architecture: Two-Tier Model

### Reference Tier (read-only, from Kaikki)

```sql
dictionary_words (
  id                  BIGSERIAL PRIMARY KEY,
  word                TEXT NOT NULL,
  pos                 TEXT NOT NULL,
  primary_translation TEXT,
  alternate_meanings  TEXT[],
  ipa                 TEXT,
  raw_data            JSONB NOT NULL
)
-- Index on (word, pos)
-- PGroonga index on word

dictionary_forms (
  id              BIGSERIAL PRIMARY KEY,
  word_id         BIGINT REFERENCES dictionary_words(id),
  form            TEXT NOT NULL,
  plain_form      TEXT NOT NULL,       -- accent marks stripped
  tags            TEXT[] NOT NULL,
  accented_form   TEXT,
  romanization    TEXT
)
-- Index on plain_form
-- Index on word_id
```

### Vocabulary Tier (study words)

Existing `lemmas` and `inflections` tables, with one addition:

- `dictionary_word_id BIGINT REFERENCES dictionary_words(id)` added to `lemmas`

Inflections populated from dictionary data instead of LLM.

## Migration Workflow

1. **Load dictionary**: Parse JSONL into `dictionary_words` + `dictionary_forms`. Strip Unicode combining acute for `plain_form`, keep original as `accented_form`. ~52K word rows, ~500-600K form rows.
2. **Export current vocabulary**: Dump 143 lemma texts to a temp file.
3. **Wipe study data**: Truncate lemmas, inflections, SRS state, study sessions, example sentences, word lists.
4. **Re-import from dictionary**: For each word, look up in dictionary, create lemma + inflections from dictionary data. 8 words not in dictionary fall back to BgGPT.

## New Word Flow (going forward)

1. Search `dictionary_forms.plain_form` for the entered word
2. If found, identify parent lemma(s) in `dictionary_words`
3. If multiple matches (e.g. пера = verb "to wash" + noun plural of перо), present all and let user choose
4. Create vocabulary entry directly from dictionary data (instant, no LLM wait)
5. If not in dictionary, fall back to BgGPT pipeline

## Design Decisions

### Multiple senses (e.g. перо = feather, plume, pen)

Option C chosen: primary meaning displayed as translation, alternates shown via tooltip. No Sense entity needed. `primary_translation` holds the first gloss, `alternate_meanings` array holds the rest.

### Stress marks

Shown by default on all inflection forms. Bulgarian textbooks for learners always mark stress. Disambiguates words like часа́ (of the hour) vs ча́са (the hour, definite). TTS (edge-tts) pronounces correctly regardless of accent marks — marks are a visual study aid only.

### No DDD, no NoSQL

The domain is simple: lookup and display. PostgreSQL JSONB gives document-store flexibility alongside relational queries. Adding MongoDB would mean operational complexity for no benefit.

## Backend Changes

### New: `DictionaryService`

- `lookupByWord(String word)` — exact match on `dictionary_words.word`
- `lookupByForm(String form)` — searches `dictionary_forms.plain_form`, returns parent dictionary words
- `getInflections(Long dictionaryWordId)` — returns all forms for a word

### New: `GET /api/dictionary/search?q=...`

Returns matching dictionary entries with lemma, pos, translation, alternate meanings. Powers frontend search/auto-suggest.

### Modified: `VocabularyService`

`createVocabulary()` checks dictionary first. If found: creates lemma + inflections immediately (status = COMPLETED, links dictionary_word_id). If not found: falls back to BgGPT pipeline.

### Modified: `LlmOrchestrationService`

Only invoked for words not in dictionary. Existing pipeline stays intact as fallback.

## Frontend Changes

### Vocabulary card

- Tooltip icon next to translation showing alternate meanings
- Stress marks on lemma heading

### Inflection tables

- Same layout (noun grid, verb conjugation, adjective grid)
- All forms display with stress marks from dictionary

### Search (biggest change)

- Search box queries `dictionary_forms` — type any inflected form, find the lemma(s)
- Results show: lemma, part of speech, primary translation
- Multiple matches presented for user selection
- Click to add to vocabulary (instant, no LLM processing)

### Homonym cross-references

- When an inflected form collides with another dictionary word, show a link
- e.g. viewing перо: "пера is also a verb — to wash"
