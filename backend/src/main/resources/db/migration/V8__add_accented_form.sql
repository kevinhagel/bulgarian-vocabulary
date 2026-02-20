ALTER TABLE inflections ADD COLUMN accented_form VARCHAR(120);
COMMENT ON COLUMN inflections.accented_form IS
  'Inflected form with Unicode stress mark (e.g. часа́). Null for pre-Phase-9 entries.';
