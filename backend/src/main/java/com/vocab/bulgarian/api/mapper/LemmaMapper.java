package com.vocab.bulgarian.api.mapper;

import com.vocab.bulgarian.api.dto.*;
import com.vocab.bulgarian.domain.Inflection;
import com.vocab.bulgarian.domain.Lemma;
import com.vocab.bulgarian.domain.enums.DifficultyLevel;
import com.vocab.bulgarian.domain.enums.PartOfSpeech;
import com.vocab.bulgarian.domain.enums.ReviewStatus;
import com.vocab.bulgarian.domain.enums.Source;
import com.vocab.bulgarian.llm.dto.LlmProcessingResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between Lemma entities and DTOs.
 * Handles complex mappings including LLM result integration and bidirectional relationships.
 */
@Mapper(componentModel = "spring")
public interface LemmaMapper {

    /**
     * Convert Lemma entity to summary response DTO (for list views).
     * Inflection count is calculated from the inflections collection size.
     */
    @Mapping(target = "inflectionCount", expression = "java(lemma.getInflections() != null ? lemma.getInflections().size() : 0)")
    LemmaResponseDTO toResponseDTO(Lemma lemma);

    /**
     * Convert Lemma entity to detail response DTO (for detail views with full inflections).
     */
    LemmaDetailDTO toDetailDTO(Lemma lemma);

    /**
     * Convert Inflection entity to DTO.
     */
    InflectionDTO toInflectionDTO(Inflection inflection);

    /**
     * Create new Lemma entity from CreateLemmaRequestDTO and LlmProcessingResult.
     * Handles complex mapping logic including:
     * - Lemma text extraction (from LLM result or fallback to user input)
     * - Enum parsing with error handling
     * - ReviewStatus based on processing success
     * - Bidirectional inflection relationship
     *
     * @param request user's create request
     * @param llmResult LLM processing result
     * @return new Lemma entity ready to persist
     */
    default Lemma toEntity(CreateLemmaRequestDTO request, LlmProcessingResult llmResult) {
        Lemma lemma = new Lemma();

        // Set lemma text: use detected lemma if available, otherwise fallback to user input
        if (llmResult.lemmaDetection() != null && !llmResult.lemmaDetection().detectionFailed()) {
            lemma.setText(llmResult.lemmaDetection().lemma());
        } else {
            lemma.setText(request.wordForm());
        }

        // Set user-provided fields
        lemma.setTranslation(request.translation());
        lemma.setNotes(request.notes());
        lemma.setSource(Source.USER_ENTERED);

        // Set LLM-generated metadata if available
        if (llmResult.metadata() != null) {
            // Parse part of speech enum with error handling
            try {
                lemma.setPartOfSpeech(PartOfSpeech.valueOf(llmResult.metadata().partOfSpeech().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Invalid enum value, leave null
                lemma.setPartOfSpeech(null);
            }

            lemma.setCategory(llmResult.metadata().category());

            // Parse difficulty level enum with error handling
            try {
                lemma.setDifficultyLevel(DifficultyLevel.valueOf(llmResult.metadata().difficultyLevel().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Invalid enum value, leave null
                lemma.setDifficultyLevel(null);
            }
        }

        // Set review status based on processing success
        if (llmResult.fullySuccessful()) {
            lemma.setReviewStatus(ReviewStatus.PENDING);
        } else {
            lemma.setReviewStatus(ReviewStatus.NEEDS_CORRECTION);
        }

        // Add inflections if available, maintaining bidirectional relationship
        if (llmResult.inflections() != null && llmResult.inflections().inflections() != null) {
            for (var entry : llmResult.inflections().inflections()) {
                Inflection inflection = new Inflection();
                inflection.setForm(entry.text());
                inflection.setGrammaticalInfo(entry.grammaticalTags());
                lemma.addInflection(inflection);
            }
        }

        return lemma;
    }

    /**
     * Update existing Lemma entity from UpdateLemmaRequestDTO.
     * Handles inflection list replacement with proper orphan removal.
     *
     * @param request update request with new data
     * @param lemma existing entity to update
     */
    default void updateEntity(UpdateLemmaRequestDTO request, Lemma lemma) {
        lemma.setText(request.text());
        lemma.setTranslation(request.translation());
        lemma.setNotes(request.notes());

        // Replace inflections if provided
        if (request.inflections() != null) {
            // Clear existing inflections (orphanRemoval will delete them)
            lemma.getInflections().clear();

            // Add new inflections
            for (InflectionUpdateDTO dto : request.inflections()) {
                Inflection inflection = new Inflection();
                inflection.setForm(dto.form());
                inflection.setGrammaticalInfo(dto.grammaticalInfo());
                lemma.addInflection(inflection);
            }
        }
    }
}
