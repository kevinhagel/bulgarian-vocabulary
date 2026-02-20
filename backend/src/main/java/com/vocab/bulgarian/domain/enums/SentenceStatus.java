package com.vocab.bulgarian.domain.enums;

public enum SentenceStatus {
    NONE,       // No sentences generated or attempted
    QUEUED,     // Generation has been queued
    GENERATING, // In progress
    DONE,       // Successfully generated
    FAILED      // Generation failed
}
