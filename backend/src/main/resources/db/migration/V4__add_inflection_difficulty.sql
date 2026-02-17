-- Add difficulty level to inflections for progressive learning
-- Allows filtering: BASIC (аз, той/тя/то) → INTERMEDIATE → ADVANCED

-- Add difficulty level column
ALTER TABLE inflections ADD COLUMN difficulty_level VARCHAR(20);

-- Add comment for documentation
COMMENT ON COLUMN inflections.difficulty_level IS 'Learning difficulty: BASIC (аз, той/тя/то only), INTERMEDIATE (add ти, ние), ADVANCED (all forms including past tenses)';

-- Set existing inflections to ADVANCED (preserve all current data)
UPDATE inflections SET difficulty_level = 'ADVANCED';
