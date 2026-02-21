-- IMMUTABLE: Never modify this file. Create new migration for updates.

-- Possessive pronouns as seed lemmas, each with a full set of inflections.
-- Uses the ADJECTIVE-style tag schema (masc/fem/neut/pl × indef/def) so the
-- frontend AdjectiveGrid renders a proper gender × definiteness table.
-- All forms are BASIC difficulty — the most important vocabulary to know.

-- ── мой (my) ────────────────────────────────────────────────────────────────
INSERT INTO lemmas (text, translation, part_of_speech, source, review_status, processing_status)
VALUES ('мой', 'my / mine', 'PRONOUN', 'SYSTEM_SEED', 'REVIEWED', 'COMPLETED');

INSERT INTO inflections (lemma_id, form, grammatical_info, difficulty_level)
SELECT id, 'мой',    'masc',     'BASIC' FROM lemmas WHERE text = 'мой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'моят',   'masc.def', 'BASIC' FROM lemmas WHERE text = 'мой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'моя',    'fem',      'BASIC' FROM lemmas WHERE text = 'мой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'моята',  'fem.def',  'BASIC' FROM lemmas WHERE text = 'мой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'мое',    'neut',     'BASIC' FROM lemmas WHERE text = 'мой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'моето',  'neut.def', 'BASIC' FROM lemmas WHERE text = 'мой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'мои',    'pl',       'BASIC' FROM lemmas WHERE text = 'мой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'моите',  'pl.def',   'BASIC' FROM lemmas WHERE text = 'мой' AND source = 'SYSTEM_SEED';

-- ── твой (your, singular informal) ──────────────────────────────────────────
INSERT INTO lemmas (text, translation, part_of_speech, source, review_status, processing_status)
VALUES ('твой', 'your / yours (singular informal)', 'PRONOUN', 'SYSTEM_SEED', 'REVIEWED', 'COMPLETED');

INSERT INTO inflections (lemma_id, form, grammatical_info, difficulty_level)
SELECT id, 'твой',   'masc',     'BASIC' FROM lemmas WHERE text = 'твой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'твоят',  'masc.def', 'BASIC' FROM lemmas WHERE text = 'твой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'твоя',   'fem',      'BASIC' FROM lemmas WHERE text = 'твой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'твоята', 'fem.def',  'BASIC' FROM lemmas WHERE text = 'твой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'твое',   'neut',     'BASIC' FROM lemmas WHERE text = 'твой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'твоето', 'neut.def', 'BASIC' FROM lemmas WHERE text = 'твой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'твои',   'pl',       'BASIC' FROM lemmas WHERE text = 'твой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'твоите', 'pl.def',   'BASIC' FROM lemmas WHERE text = 'твой' AND source = 'SYSTEM_SEED';

-- ── негов (his) ──────────────────────────────────────────────────────────────
INSERT INTO lemmas (text, translation, part_of_speech, source, review_status, processing_status)
VALUES ('негов', 'his', 'PRONOUN', 'SYSTEM_SEED', 'REVIEWED', 'COMPLETED');

INSERT INTO inflections (lemma_id, form, grammatical_info, difficulty_level)
SELECT id, 'негов',     'masc',     'BASIC' FROM lemmas WHERE text = 'негов' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'неговият',  'masc.def', 'BASIC' FROM lemmas WHERE text = 'негов' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'негова',    'fem',      'BASIC' FROM lemmas WHERE text = 'негов' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'неговата',  'fem.def',  'BASIC' FROM lemmas WHERE text = 'негов' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'негово',    'neut',     'BASIC' FROM lemmas WHERE text = 'негов' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'неговото',  'neut.def', 'BASIC' FROM lemmas WHERE text = 'негов' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'негови',    'pl',       'BASIC' FROM lemmas WHERE text = 'негов' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'неговите',  'pl.def',   'BASIC' FROM lemmas WHERE text = 'негов' AND source = 'SYSTEM_SEED';

-- ── нейн (her) ───────────────────────────────────────────────────────────────
INSERT INTO lemmas (text, translation, part_of_speech, source, review_status, processing_status)
VALUES ('нейн', 'her / hers', 'PRONOUN', 'SYSTEM_SEED', 'REVIEWED', 'COMPLETED');

INSERT INTO inflections (lemma_id, form, grammatical_info, difficulty_level)
SELECT id, 'нейн',    'masc',     'BASIC' FROM lemmas WHERE text = 'нейн' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'нейният', 'masc.def', 'BASIC' FROM lemmas WHERE text = 'нейн' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'нейна',   'fem',      'BASIC' FROM lemmas WHERE text = 'нейн' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'нейната', 'fem.def',  'BASIC' FROM lemmas WHERE text = 'нейн' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'нейно',   'neut',     'BASIC' FROM lemmas WHERE text = 'нейн' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'нейното', 'neut.def', 'BASIC' FROM lemmas WHERE text = 'нейн' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'нейни',   'pl',       'BASIC' FROM lemmas WHERE text = 'нейн' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'нейните', 'pl.def',   'BASIC' FROM lemmas WHERE text = 'нейн' AND source = 'SYSTEM_SEED';

-- ── наш (our) ────────────────────────────────────────────────────────────────
INSERT INTO lemmas (text, translation, part_of_speech, source, review_status, processing_status)
VALUES ('наш', 'our / ours', 'PRONOUN', 'SYSTEM_SEED', 'REVIEWED', 'COMPLETED');

INSERT INTO inflections (lemma_id, form, grammatical_info, difficulty_level)
SELECT id, 'наш',    'masc',     'BASIC' FROM lemmas WHERE text = 'наш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'нашият', 'masc.def', 'BASIC' FROM lemmas WHERE text = 'наш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'наша',   'fem',      'BASIC' FROM lemmas WHERE text = 'наш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'нашата', 'fem.def',  'BASIC' FROM lemmas WHERE text = 'наш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'наше',   'neut',     'BASIC' FROM lemmas WHERE text = 'наш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'нашето', 'neut.def', 'BASIC' FROM lemmas WHERE text = 'наш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'наши',   'pl',       'BASIC' FROM lemmas WHERE text = 'наш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'нашите', 'pl.def',   'BASIC' FROM lemmas WHERE text = 'наш' AND source = 'SYSTEM_SEED';

-- ── ваш (your, plural / formal) ──────────────────────────────────────────────
INSERT INTO lemmas (text, translation, part_of_speech, source, review_status, processing_status)
VALUES ('ваш', 'your / yours (plural or formal)', 'PRONOUN', 'SYSTEM_SEED', 'REVIEWED', 'COMPLETED');

INSERT INTO inflections (lemma_id, form, grammatical_info, difficulty_level)
SELECT id, 'ваш',    'masc',     'BASIC' FROM lemmas WHERE text = 'ваш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'вашият', 'masc.def', 'BASIC' FROM lemmas WHERE text = 'ваш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'ваша',   'fem',      'BASIC' FROM lemmas WHERE text = 'ваш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'вашата', 'fem.def',  'BASIC' FROM lemmas WHERE text = 'ваш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'ваше',   'neut',     'BASIC' FROM lemmas WHERE text = 'ваш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'вашето', 'neut.def', 'BASIC' FROM lemmas WHERE text = 'ваш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'ваши',   'pl',       'BASIC' FROM lemmas WHERE text = 'ваш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'вашите', 'pl.def',   'BASIC' FROM lemmas WHERE text = 'ваш' AND source = 'SYSTEM_SEED';

-- ── техен (their) ────────────────────────────────────────────────────────────
INSERT INTO lemmas (text, translation, part_of_speech, source, review_status, processing_status)
VALUES ('техен', 'their / theirs', 'PRONOUN', 'SYSTEM_SEED', 'REVIEWED', 'COMPLETED');

INSERT INTO inflections (lemma_id, form, grammatical_info, difficulty_level)
SELECT id, 'техен',   'masc',     'BASIC' FROM lemmas WHERE text = 'техен' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'техният', 'masc.def', 'BASIC' FROM lemmas WHERE text = 'техен' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'тяхна',   'fem',      'BASIC' FROM lemmas WHERE text = 'техен' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'тяхната', 'fem.def',  'BASIC' FROM lemmas WHERE text = 'техен' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'тяхно',   'neut',     'BASIC' FROM lemmas WHERE text = 'техен' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'тяхното', 'neut.def', 'BASIC' FROM lemmas WHERE text = 'техен' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'техни',   'pl',       'BASIC' FROM lemmas WHERE text = 'техен' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'техните', 'pl.def',   'BASIC' FROM lemmas WHERE text = 'техен' AND source = 'SYSTEM_SEED';

-- ── свой (one's own — reflexive possessive) ──────────────────────────────────
INSERT INTO lemmas (text, translation, part_of_speech, source, review_status, processing_status)
VALUES ('свой', 'one''s own (reflexive possessive)', 'PRONOUN', 'SYSTEM_SEED', 'REVIEWED', 'COMPLETED');

INSERT INTO inflections (lemma_id, form, grammatical_info, difficulty_level)
SELECT id, 'свой',   'masc',     'BASIC' FROM lemmas WHERE text = 'свой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'своят',  'masc.def', 'BASIC' FROM lemmas WHERE text = 'свой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'своя',   'fem',      'BASIC' FROM lemmas WHERE text = 'свой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'своята', 'fem.def',  'BASIC' FROM lemmas WHERE text = 'свой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'свое',   'neut',     'BASIC' FROM lemmas WHERE text = 'свой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'своето', 'neut.def', 'BASIC' FROM lemmas WHERE text = 'свой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'свои',   'pl',       'BASIC' FROM lemmas WHERE text = 'свой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'своите', 'pl.def',   'BASIC' FROM lemmas WHERE text = 'свой' AND source = 'SYSTEM_SEED';
