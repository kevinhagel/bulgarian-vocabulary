-- Add background processing support for LLM pipeline
-- Allows translation to be auto-generated and tracks processing status

-- Make translation nullable (will be filled by background processor)
ALTER TABLE lemmas ALTER COLUMN translation DROP NOT NULL;

-- Add processing status tracking
ALTER TABLE lemmas ADD COLUMN processing_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED';

-- Add processing error message for debugging failed jobs
ALTER TABLE lemmas ADD COLUMN processing_error TEXT;

-- Add comment for documentation
COMMENT ON COLUMN lemmas.processing_status IS 'Background processing status: QUEUED, PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN lemmas.processing_error IS 'Error message if processing_status is FAILED';

-- Existing entries are already complete
UPDATE lemmas SET processing_status = 'COMPLETED' WHERE translation IS NOT NULL;
