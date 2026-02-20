-- Add missing preposition 'през' (through/during) omitted from V2 seed data
INSERT INTO lemmas (text, translation, source, review_status)
SELECT 'през', 'through/during', 'SYSTEM_SEED', 'REVIEWED'
WHERE NOT EXISTS (SELECT 1 FROM lemmas WHERE text = 'през');
