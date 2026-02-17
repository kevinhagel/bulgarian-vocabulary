package com.vocab.bulgarian.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new lemma entry.
 * User enters Bulgarian word in any form (wordForm) which will be processed for lemma detection.
 * Translation is optional - if not provided, will be auto-generated via Google Translate.
 * Supports multi-word phrases.
 */
public record CreateLemmaRequestDTO(
    @NotBlank(groups = OnCreate.class)
    @Size(min = 1, max = 100)
    String wordForm,

    @Size(min = 1, max = 200)
    String translation, // Optional - auto-generated if null

    @Size(max = 5000)
    String notes
) {
}
