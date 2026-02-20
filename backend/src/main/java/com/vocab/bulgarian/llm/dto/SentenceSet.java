package com.vocab.bulgarian.llm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * LLM output DTO for example sentence generation.
 * Returned by Qwen 2.5 14B when asked to generate Bulgarian example sentences.
 */
public record SentenceSet(
    @NotBlank String lemma,
    @NotEmpty List<SentenceEntry> sentences
) {
    public record SentenceEntry(
        @NotBlank String bulgarianText,
        @NotBlank String englishTranslation
    ) {}
}
