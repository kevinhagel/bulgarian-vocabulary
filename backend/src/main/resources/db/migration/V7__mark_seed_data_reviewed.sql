-- Phase 8: Mark all system seed data as REVIEWED so it is eligible for SRS study sessions.
-- User-entered words start as PENDING (requiring human review) but seed data
-- has already been curated and can go directly into the study queue.
UPDATE lemmas
SET review_status = 'REVIEWED'
WHERE source = 'SYSTEM_SEED'
  AND review_status = 'PENDING';
