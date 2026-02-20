package com.vocab.bulgarian.api.dto;

import com.vocab.bulgarian.domain.enums.DifficultyLevel;
import com.vocab.bulgarian.domain.enums.PartOfSpeech;
import com.vocab.bulgarian.domain.enums.ReviewStatus;
import com.vocab.bulgarian.domain.enums.SentenceStatus;
import com.vocab.bulgarian.domain.enums.Source;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for lemma detail view.
 * Contains full lemma data including inflections and example sentences.
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
    List<ExampleSentenceDTO> exampleSentences,
    SentenceStatus sentenceStatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
