package com.vocab.bulgarian.llm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Structured output for LLM-based inflection generation.
 * Represents all inflected forms for a given lemma.
 */
public record InflectionSet(
    @NotBlank String lemma,
    @NotBlank String partOfSpeech,
    @NotEmpty List<InflectionEntry> inflections
) {
    /**
     * Individual inflection entry with grammatical tags.
     */
    public record InflectionEntry(
        @NotBlank String text,          // The inflected form (Cyrillic)
        String grammaticalTags          // e.g., "1sg.pres.perf", "3pl.past.imperf"
    ) {}
}
