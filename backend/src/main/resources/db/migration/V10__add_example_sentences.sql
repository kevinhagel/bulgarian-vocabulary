-- Add example sentences table and sentence_status column to lemmas

CREATE TABLE example_sentences (
    id BIGSERIAL PRIMARY KEY,
    lemma_id BIGINT NOT NULL REFERENCES lemmas(id) ON DELETE CASCADE,
    bulgarian_text TEXT NOT NULL,
    english_translation TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_example_sentences_lemma_id ON example_sentences(lemma_id);

ALTER TABLE lemmas
    ADD COLUMN sentence_status VARCHAR(20) NOT NULL DEFAULT 'NONE';
