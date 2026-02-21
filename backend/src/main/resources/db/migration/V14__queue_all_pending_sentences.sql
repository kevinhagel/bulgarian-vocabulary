-- IMMUTABLE: Never modify this file. Create new migration for updates.

-- Mark all COMPLETED lemmas that have no sentences yet as QUEUED.
-- On next startup, StartupSentenceService will fire backgroundGenerateSentences
-- for all QUEUED words, working through the backlog without any manual UI clicks.

UPDATE lemmas
SET sentence_status = 'QUEUED'
WHERE sentence_status = 'NONE'
  AND processing_status = 'COMPLETED';
