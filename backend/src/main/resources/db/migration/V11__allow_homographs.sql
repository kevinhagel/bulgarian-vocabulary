-- Allow the same word form to appear multiple times when notes differ.
-- Example: път (road) and път (time/occasion) are distinct lexical entries.
--
-- Old constraint: UNIQUE(text, source) — blocked all same-word duplicates.
-- New constraint: UNIQUE(text, source, COALESCE(notes, '')) — allows same
-- word only when the notes field differs. Two entries with the same text,
-- same source, and BOTH null/empty notes still conflict (true duplicates).

DROP INDEX IF EXISTS idx_lemmas_text_source;

CREATE UNIQUE INDEX idx_lemmas_text_source_notes
    ON lemmas(text, source, COALESCE(notes, ''));
