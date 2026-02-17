package com.vocab.bulgarian.api.dto;

/**
 * Response DTO representing an inflected form of a lemma.
 * Includes difficulty level for progressive learning (BASIC, INTERMEDIATE, ADVANCED).
 */
public record InflectionDTO(
    Long id,
    String form,
    String grammaticalInfo,
    String difficultyLevel  // BASIC (аз, той/тя/то), INTERMEDIATE, ADVANCED
) {
}
