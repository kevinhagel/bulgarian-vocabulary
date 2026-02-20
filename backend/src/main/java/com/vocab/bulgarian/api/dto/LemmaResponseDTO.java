package com.vocab.bulgarian.api.dto;

import com.vocab.bulgarian.domain.enums.DifficultyLevel;
import com.vocab.bulgarian.domain.enums.PartOfSpeech;
import com.vocab.bulgarian.domain.enums.ReviewStatus;
import com.vocab.bulgarian.domain.enums.SentenceStatus;
import com.vocab.bulgarian.domain.enums.Source;

import java.time.LocalDateTime;

/**
 * Response DTO for lemma list/summary view.
 * Contains summary data and inflection count (not full inflections list).
 */
public record LemmaResponseDTO(
    Long id,
    String text,
    String translation,
    PartOfSpeech partOfSpeech,
    String category,
    DifficultyLevel difficultyLevel,
    Source source,
    ReviewStatus reviewStatus,
    int inflectionCount,
    SentenceStatus sentenceStatus,
    LocalDateTime createdAt
) {
}
