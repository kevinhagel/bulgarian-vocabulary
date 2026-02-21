-- IMMUTABLE: Never modify this file. Create new migration for updates.

-- Example sentences for the possessive pronoun seeds added in V12.
-- Written directly rather than relying on LLM batch generation,
-- which processes oldest-first and the pronouns are at positions 94-100 in the queue.

-- ── твой ────────────────────────────────────────────────────────────────────
INSERT INTO example_sentences (lemma_id, bulgarian_text, english_translation, sort_order)
SELECT id, 'Твоят брат е много висок.', 'Your brother is very tall.', 0
FROM lemmas WHERE text = 'твой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Харесва ли ти твоята нова работа?', 'Do you like your new job?', 1
FROM lemmas WHERE text = 'твой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Твоите думи ме изненадаха.', 'Your words surprised me.', 2
FROM lemmas WHERE text = 'твой' AND source = 'SYSTEM_SEED';

UPDATE lemmas SET sentence_status = 'DONE' WHERE text = 'твой' AND source = 'SYSTEM_SEED';

-- ── негов ────────────────────────────────────────────────────────────────────
INSERT INTO example_sentences (lemma_id, bulgarian_text, english_translation, sort_order)
SELECT id, 'Неговият автомобил е паркиран пред сградата.', 'His car is parked in front of the building.', 0
FROM lemmas WHERE text = 'негов' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Негова дъщеря учи медицина.', 'His daughter studies medicine.', 1
FROM lemmas WHERE text = 'негов' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Неговите приятели го посетиха вчера.', 'His friends visited him yesterday.', 2
FROM lemmas WHERE text = 'негов' AND source = 'SYSTEM_SEED';

UPDATE lemmas SET sentence_status = 'DONE' WHERE text = 'негов' AND source = 'SYSTEM_SEED';

-- ── нейн ─────────────────────────────────────────────────────────────────────
INSERT INTO example_sentences (lemma_id, bulgarian_text, english_translation, sort_order)
SELECT id, 'Нейната стая е много уютна.', 'Her room is very cozy.', 0
FROM lemmas WHERE text = 'нейн' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Нейният приятел я изненада с цветя.', 'Her boyfriend surprised her with flowers.', 1
FROM lemmas WHERE text = 'нейн' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Нейните книги заемат цял рафт.', 'Her books take up a whole shelf.', 2
FROM lemmas WHERE text = 'нейн' AND source = 'SYSTEM_SEED';

UPDATE lemmas SET sentence_status = 'DONE' WHERE text = 'нейн' AND source = 'SYSTEM_SEED';

-- ── наш ──────────────────────────────────────────────────────────────────────
INSERT INTO example_sentences (lemma_id, bulgarian_text, english_translation, sort_order)
SELECT id, 'Нашият град е много красив.', 'Our city is very beautiful.', 0
FROM lemmas WHERE text = 'наш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Нашата кола се повреди по пътя.', 'Our car broke down on the way.', 1
FROM lemmas WHERE text = 'наш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Нашите деца учат в едно училище.', 'Our children go to the same school.', 2
FROM lemmas WHERE text = 'наш' AND source = 'SYSTEM_SEED';

UPDATE lemmas SET sentence_status = 'DONE' WHERE text = 'наш' AND source = 'SYSTEM_SEED';

-- ── ваш ──────────────────────────────────────────────────────────────────────
INSERT INTO example_sentences (lemma_id, bulgarian_text, english_translation, sort_order)
SELECT id, 'Вашата компания работи ли в чужбина?', 'Does your company work abroad?', 0
FROM lemmas WHERE text = 'ваш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Вашият директор беше ли на срещата?', 'Was your director at the meeting?', 1
FROM lemmas WHERE text = 'ваш' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Вашите предложения са много интересни.', 'Your proposals are very interesting.', 2
FROM lemmas WHERE text = 'ваш' AND source = 'SYSTEM_SEED';

UPDATE lemmas SET sentence_status = 'DONE' WHERE text = 'ваш' AND source = 'SYSTEM_SEED';

-- ── техен ────────────────────────────────────────────────────────────────────
INSERT INTO example_sentences (lemma_id, bulgarian_text, english_translation, sort_order)
SELECT id, 'Тяхната идея беше приета от всички.', 'Their idea was accepted by everyone.', 0
FROM lemmas WHERE text = 'техен' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Техният дом се намира в покрайнините на града.', 'Their home is on the outskirts of the city.', 1
FROM lemmas WHERE text = 'техен' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Техните деца говорят три езика.', 'Their children speak three languages.', 2
FROM lemmas WHERE text = 'техен' AND source = 'SYSTEM_SEED';

UPDATE lemmas SET sentence_status = 'DONE' WHERE text = 'техен' AND source = 'SYSTEM_SEED';

-- ── свой ─────────────────────────────────────────────────────────────────────
INSERT INTO example_sentences (lemma_id, bulgarian_text, english_translation, sort_order)
SELECT id, 'Всеки трябва да уважава своите родители.', 'Everyone should respect their own parents.', 0
FROM lemmas WHERE text = 'свой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Тя отиде в своята стая и затвори вратата.', 'She went to her own room and closed the door.', 1
FROM lemmas WHERE text = 'свой' AND source = 'SYSTEM_SEED' UNION ALL
SELECT id, 'Той написа своите мисли в дневника.', 'He wrote his own thoughts in his diary.', 2
FROM lemmas WHERE text = 'свой' AND source = 'SYSTEM_SEED';

UPDATE lemmas SET sentence_status = 'DONE' WHERE text = 'свой' AND source = 'SYSTEM_SEED';
