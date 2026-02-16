package com.vocab.bulgarian.domain.enums;

/**
 * Review status for LLM-generated metadata.
 * PENDING: Awaiting LLM processing or human review
 * REVIEWED: Verified and approved
 * NEEDS_CORRECTION: Flagged for correction
 */
public enum ReviewStatus {
    PENDING,
    REVIEWED,
    NEEDS_CORRECTION
}
