package com.vocab.bulgarian.api.dto;

/**
 * Response DTO representing an inflected form of a lemma.
 */
public record InflectionDTO(
    Long id,
    String form,
    String grammaticalInfo
) {
}
