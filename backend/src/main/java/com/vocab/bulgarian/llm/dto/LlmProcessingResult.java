package com.vocab.bulgarian.llm.dto;

import java.util.List;

/**
 * Result of the complete LLM processing pipeline for a new vocabulary entry.
 * Combines lemma detection, inflection generation, and metadata generation results.
 */
public record LlmProcessingResult(
    String originalWordForm,
    LemmaDetectionResponse lemmaDetection,
    InflectionSet inflections,          // nullable -- generation may fail
    LemmaMetadata metadata,             // nullable -- generation may fail
    boolean fullySuccessful,            // true only if all 3 succeeded
    List<String> warnings               // partial failure messages
) {}
