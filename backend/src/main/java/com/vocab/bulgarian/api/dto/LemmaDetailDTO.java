package com.vocab.bulgarian.api.dto;

import com.vocab.bulgarian.domain.enums.DifficultyLevel;
import com.vocab.bulgarian.domain.enums.PartOfSpeech;
import com.vocab.bulgarian.domain.enums.ReviewStatus;
import com.vocab.bulgarian.domain.enums.Source;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for lemma detail view.
 * Contains full lemma data including the complete list of inflections.
 */
public record LemmaDetailDTO(
    Long id,
    String text,
    String translation,
    String notes,
    PartOfSpeech partOfSpeech,
    String category,
    DifficultyLevel difficultyLevel,
    Source source,
    ReviewStatus reviewStatus,
    List<InflectionDTO> inflections,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
