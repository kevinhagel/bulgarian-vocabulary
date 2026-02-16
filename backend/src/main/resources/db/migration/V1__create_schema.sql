-- Enable PGroonga extension for Cyrillic full-text search
CREATE EXTENSION IF NOT EXISTS pgroonga;

CREATE TABLE lemmas (
    id BIGSERIAL PRIMARY KEY,
    text VARCHAR(100) NOT NULL,           -- Bulgarian lemma text (e.g., "говоря")
    translation VARCHAR(200) NOT NULL,    -- English translation
    notes TEXT,                           -- User-editable notes
    part_of_speech VARCHAR(30),           -- NOUN, VERB, ADJECTIVE, etc. (nullable: LLM fills in Phase 2)
    category VARCHAR(50),                 -- e.g., "food", "travel" (nullable: LLM fills in Phase 2)
    difficulty_level VARCHAR(20),         -- BEGINNER, INTERMEDIATE, ADVANCED (nullable: LLM fills in Phase 2)
    source VARCHAR(20) NOT NULL,          -- USER_ENTERED or SYSTEM_SEED
    review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, REVIEWED, NEEDS_CORRECTION
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inflections (
    id BIGSERIAL PRIMARY KEY,
    lemma_id BIGINT NOT NULL REFERENCES lemmas(id) ON DELETE CASCADE,
    form VARCHAR(100) NOT NULL,           -- Inflected form (e.g., "говориш")
    grammatical_info VARCHAR(100),        -- e.g., "2nd person singular present indicative"
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- PGroonga index for Bulgarian Cyrillic full-text search on lemma text
CREATE INDEX idx_lemmas_text_pgroonga ON lemmas USING pgroonga(text);

-- PGroonga index on inflection forms for searching inflected forms
CREATE INDEX idx_inflections_form_pgroonga ON inflections USING pgroonga(form);

-- Standard B-tree indexes for foreign keys and filtered queries
CREATE INDEX idx_inflections_lemma_id ON inflections(lemma_id);
CREATE INDEX idx_lemmas_source ON lemmas(source);
CREATE INDEX idx_lemmas_part_of_speech ON lemmas(part_of_speech);
CREATE INDEX idx_lemmas_difficulty_level ON lemmas(difficulty_level);
CREATE INDEX idx_lemmas_review_status ON lemmas(review_status);

-- Unique constraint: prevent duplicate lemma text per source
CREATE UNIQUE INDEX idx_lemmas_text_source ON lemmas(text, source);
