-- IMMUTABLE: Never modify this file. Create new migration for updates (e.g., V5__update_pronouns.sql).

-- Seed reference vocabulary: interrogatives, pronouns, prepositions, conjunctions, numerals
-- All seeded with source = 'SYSTEM_SEED' and review_status = 'REVIEWED' (pre-verified reference data)

-- Interrogatives
INSERT INTO lemmas (text, translation, source, review_status) VALUES
('кой', 'who', 'SYSTEM_SEED', 'REVIEWED'),
('какво', 'what', 'SYSTEM_SEED', 'REVIEWED'),
('кога', 'when', 'SYSTEM_SEED', 'REVIEWED'),
('къде', 'where', 'SYSTEM_SEED', 'REVIEWED'),
('защо', 'why', 'SYSTEM_SEED', 'REVIEWED'),
('как', 'how', 'SYSTEM_SEED', 'REVIEWED'),
('колко', 'how many/how much', 'SYSTEM_SEED', 'REVIEWED'),
('чий', 'whose', 'SYSTEM_SEED', 'REVIEWED');

-- Personal Pronouns
INSERT INTO lemmas (text, translation, source, review_status) VALUES
('аз', 'I', 'SYSTEM_SEED', 'REVIEWED'),
('ти', 'you (informal)', 'SYSTEM_SEED', 'REVIEWED'),
('той', 'he', 'SYSTEM_SEED', 'REVIEWED'),
('тя', 'she', 'SYSTEM_SEED', 'REVIEWED'),
('то', 'it', 'SYSTEM_SEED', 'REVIEWED'),
('ние', 'we', 'SYSTEM_SEED', 'REVIEWED'),
('вие', 'you (formal/plural)', 'SYSTEM_SEED', 'REVIEWED'),
('те', 'they', 'SYSTEM_SEED', 'REVIEWED');

-- Prepositions
INSERT INTO lemmas (text, translation, source, review_status) VALUES
('на', 'on/to', 'SYSTEM_SEED', 'REVIEWED'),
('в', 'in', 'SYSTEM_SEED', 'REVIEWED'),
('във', 'in (variant)', 'SYSTEM_SEED', 'REVIEWED'),
('с', 'with', 'SYSTEM_SEED', 'REVIEWED'),
('със', 'with (variant)', 'SYSTEM_SEED', 'REVIEWED'),
('за', 'for', 'SYSTEM_SEED', 'REVIEWED'),
('от', 'from', 'SYSTEM_SEED', 'REVIEWED'),
('до', 'to/until', 'SYSTEM_SEED', 'REVIEWED'),
('без', 'without', 'SYSTEM_SEED', 'REVIEWED'),
('между', 'between', 'SYSTEM_SEED', 'REVIEWED'),
('над', 'above', 'SYSTEM_SEED', 'REVIEWED'),
('под', 'below', 'SYSTEM_SEED', 'REVIEWED'),
('след', 'after', 'SYSTEM_SEED', 'REVIEWED'),
('преди', 'before', 'SYSTEM_SEED', 'REVIEWED'),
('при', 'at/by', 'SYSTEM_SEED', 'REVIEWED'),
('около', 'around', 'SYSTEM_SEED', 'REVIEWED');

-- Conjunctions
INSERT INTO lemmas (text, translation, source, review_status) VALUES
('и', 'and', 'SYSTEM_SEED', 'REVIEWED'),
('но', 'but', 'SYSTEM_SEED', 'REVIEWED'),
('или', 'or', 'SYSTEM_SEED', 'REVIEWED'),
('а', 'and/but', 'SYSTEM_SEED', 'REVIEWED'),
('ако', 'if', 'SYSTEM_SEED', 'REVIEWED'),
('защото', 'because', 'SYSTEM_SEED', 'REVIEWED'),
('когато', 'when', 'SYSTEM_SEED', 'REVIEWED'),
('че', 'that', 'SYSTEM_SEED', 'REVIEWED'),
('макар че', 'although', 'SYSTEM_SEED', 'REVIEWED'),
('докато', 'while', 'SYSTEM_SEED', 'REVIEWED');

-- Numerals 1-10
INSERT INTO lemmas (text, translation, source, review_status) VALUES
('едно', 'one', 'SYSTEM_SEED', 'REVIEWED'),
('две', 'two', 'SYSTEM_SEED', 'REVIEWED'),
('три', 'three', 'SYSTEM_SEED', 'REVIEWED'),
('четири', 'four', 'SYSTEM_SEED', 'REVIEWED'),
('пет', 'five', 'SYSTEM_SEED', 'REVIEWED'),
('шест', 'six', 'SYSTEM_SEED', 'REVIEWED'),
('седем', 'seven', 'SYSTEM_SEED', 'REVIEWED'),
('осем', 'eight', 'SYSTEM_SEED', 'REVIEWED'),
('девет', 'nine', 'SYSTEM_SEED', 'REVIEWED'),
('десет', 'ten', 'SYSTEM_SEED', 'REVIEWED');
