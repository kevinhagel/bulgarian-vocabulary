package com.vocab.bulgarian.domain.enums;

/**
 * Processing status for LLM background processing pipeline.
 * Tracks the state of asynchronous vocabulary entry processing.
 */
public enum ProcessingStatus {
    /**
     * Entry saved, waiting to start background processing.
     */
    QUEUED,

    /**
     * Background processing in progress (lemma detection, translation, inflections, metadata).
     */
    PROCESSING,

    /**
     * Background processing completed successfully.
     */
    COMPLETED,

    /**
     * Background processing failed (will not retry automatically).
     */
    FAILED
}
