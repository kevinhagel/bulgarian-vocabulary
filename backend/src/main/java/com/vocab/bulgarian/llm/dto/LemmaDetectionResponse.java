package com.vocab.bulgarian.llm.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Structured output for LLM-based lemma detection.
 * Represents the detected lemma (dictionary form) and part of speech for a given word form.
 */
public record LemmaDetectionResponse(
    @NotBlank String wordForm,       // The original word form submitted
    @NotBlank String lemma,          // Detected lemma (dictionary form)
    String partOfSpeech,             // LLM's detected part of speech
    boolean detectionFailed          // True if detection failed (for fallback)
) {
    /**
     * Static factory method for creating a failed detection response.
     * Used when LLM call fails or returns invalid data.
     */
    public static LemmaDetectionResponse failed(String wordForm) {
        return new LemmaDetectionResponse(wordForm, null, null, true);
    }
}
