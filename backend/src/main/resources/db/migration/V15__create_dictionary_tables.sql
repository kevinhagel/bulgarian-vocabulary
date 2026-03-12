-- IMMUTABLE: Never modify this file. Create new migration for updates.

-- Dictionary reference tables for Kaikki/Wiktionary Bulgarian dictionary data.
-- Two-tier model: dictionary_words + dictionary_forms (read-only reference tier)
-- linked to existing lemmas table (vocabulary/study tier) via dictionary_word_id FK.

-- Reference tier: canonical dictionary entries
CREATE TABLE dictionary_words (
    id                  BIGSERIAL PRIMARY KEY,
    word                TEXT NOT NULL,
    pos                 TEXT NOT NULL,
    primary_translation TEXT,
    alternate_meanings  TEXT[],
    ipa                 TEXT,
    raw_data            JSONB NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Composite index for lookup by word+pos (handles homonyms)
CREATE INDEX idx_dictionary_words_word_pos ON dictionary_words (word, pos);

-- PGroonga full-text index for Bulgarian Cyrillic search
CREATE INDEX idx_dictionary_words_pgroonga ON dictionary_words USING pgroonga (word);

-- Reference tier: all inflected forms, linking back to their dictionary word
CREATE TABLE dictionary_forms (
    id              BIGSERIAL PRIMARY KEY,
    word_id         BIGINT NOT NULL REFERENCES dictionary_words(id) ON DELETE CASCADE,
    form            TEXT NOT NULL,
    plain_form      TEXT NOT NULL,
    tags            TEXT[] NOT NULL,
    accented_form   TEXT,
    romanization    TEXT
);

-- Primary lookup index: search by plain (unaccented) form
CREATE INDEX idx_dictionary_forms_plain_form ON dictionary_forms (plain_form);

-- PGroonga index for fuzzy/prefix search on plain form
CREATE INDEX idx_dictionary_forms_pgroonga ON dictionary_forms USING pgroonga (plain_form);

-- FK index for joining back to dictionary_words
CREATE INDEX idx_dictionary_forms_word_id ON dictionary_forms (word_id);

-- Link vocabulary tier to reference tier
ALTER TABLE lemmas ADD COLUMN dictionary_word_id BIGINT REFERENCES dictionary_words(id);
CREATE INDEX idx_lemmas_dictionary_word_id ON lemmas (dictionary_word_id);
