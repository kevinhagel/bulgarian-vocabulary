package com.vocab.bulgarian.api.dto;

/**
 * DTO representing a single example sentence for a Bulgarian lemma.
 */
public record ExampleSentenceDTO(
    Long id,
    String bulgarianText,
    String englishTranslation,
    int sortOrder
) {
}
