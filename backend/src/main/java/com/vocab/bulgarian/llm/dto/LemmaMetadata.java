package com.vocab.bulgarian.llm.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Structured output for LLM-based metadata generation.
 * Represents category classification and difficulty assessment for a lemma.
 */
public record LemmaMetadata(
    @NotBlank String lemma,
    @NotBlank String partOfSpeech,     // Matches PartOfSpeech enum values
    String category,                    // Topic category (e.g., "food", "travel")
    @NotBlank String difficultyLevel   // Matches DifficultyLevel enum values
) {}
